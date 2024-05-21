package tsp.headdb.implementation.requester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tsp.headdb.HeadDB;
import tsp.headdb.core.util.Utils;
import tsp.headdb.implementation.category.Category;
import tsp.headdb.implementation.head.Head;

/**
 * Responsible for requesting heads from providers.
 *
 * @author TheSilentPro
 * @see tsp.headdb.core.api.HeadAPI
 * @see tsp.headdb.implementation.head.HeadDatabase
 */
public class Requester {

    private final JavaPlugin plugin;
    private HeadProvider provider;

    public Requester(final JavaPlugin plugin, final HeadProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    public List<Head> fetchAndResolve(final Category category) {
        try {
            final Response response = this.fetch(category);
            final List<Head> result = new ArrayList<>();
            if (response.code() != 200) {
                return result;
            }

            final JsonArray main = JsonParser.parseString(response.response()).getAsJsonArray();
            for (final JsonElement entry : main) {
                final JsonObject obj = entry.getAsJsonObject();
                final int id = obj.get("id").getAsInt();

                if (this.plugin.getConfig().contains("blockedHeads.ids")) {
                    final List<Integer> blockedIds = this.plugin.getConfig().getIntegerList("blockedHeads.ids");
                    if (blockedIds.contains(id)) {
                        HeadDB.getInstance().getLog().debug("Skipped blocked head: " + obj.get("name").getAsString() + "(" + id + ")");
                        continue;
                    }
                }

                result.add(new Head(id, Utils.validateUniqueId(obj.get("uuid").getAsString()).orElse(UUID.randomUUID()),
                        obj.get("name").getAsString(), obj.get("value").getAsString(), obj.get("tags").getAsString(), response.date(),
                        category));
            }

            return result;
        } catch (final IOException ex) {
            HeadDB.getInstance().getLog().debug("Failed to load from provider: " + this.provider.name());
            if (HeadDB.getInstance().getConfig().getBoolean("fallback") && this.provider != HeadProvider.HEAD_ARCHIVE) { // prevent
                                                                                                                         // recursion.
                // Maybe switch to an
                // attempts counter down
                // in the future
                this.provider = HeadProvider.HEAD_ARCHIVE;
                return this.fetchAndResolve(category);
            } else {
                HeadDB.getInstance().getLog().error("Could not fetch heads from any provider!");
                return new ArrayList<>();
            }
        }
    }

    public Response fetch(final Category category) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) URI.create(this.provider.getFormattedUrl(category)).toURL()
                .openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent",
                this.plugin.getName() + "/" + Utils.getVersion().orElse(this.plugin.getPluginMeta().getVersion()));
        connection.setRequestProperty("Accept", "application/json");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            connection.disconnect();
            return new Response(builder.toString(), connection.getResponseCode(), connection.getHeaderField("date"));
        }
    }

    public HeadProvider getProvider() { return this.provider; }

    public JavaPlugin getPlugin() { return this.plugin; }

}
