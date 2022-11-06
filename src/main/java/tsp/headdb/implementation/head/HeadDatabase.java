package tsp.headdb.implementation.head;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import tsp.headdb.HeadDB;
import tsp.headdb.implementation.category.Category;
import tsp.headdb.implementation.requester.HeadProvider;
import tsp.headdb.implementation.requester.Requester;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HeadDatabase {

    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final Requester requester;
    private final Map<Category, List<Head>> heads;
    private long timestamp;

    public HeadDatabase(JavaPlugin plugin, HeadProvider provider) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        this.requester = new Requester(plugin, provider);
        this.heads = Collections.synchronizedMap(new EnumMap<>(Category.class));
    }

    public Map<Category, List<Head>> getHeads() {
        return heads;
    }

    public void getHeadsNoCache(BiConsumer<Long, Map<Category, List<Head>>> heads) {
        getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            Map<Category, List<Head>> result = new HashMap<>();
            for (Category category : Category.VALUES) {
                requester.fetchAndResolve(category, response -> result.put(category, response));
            }

            heads.accept(System.currentTimeMillis() - start, result);
        });
    }

    public void update(BiConsumer<Long, Map<Category, List<Head>>> fetched) {
        getHeadsNoCache((elapsed, result) -> {
            synchronized (heads) {
                heads.putAll(result);
            }
            timestamp = System.currentTimeMillis();
            fetched.accept(elapsed, result);
        });
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    public Requester getRequester() {
        return requester;
    }

}