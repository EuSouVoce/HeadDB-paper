package tsp.headdb.core.command;

import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tsp.headdb.HeadDB;
import tsp.headdb.core.api.HeadAPI;
import tsp.headdb.core.util.Utils;
import tsp.headdb.implementation.category.Category;
import tsp.headdb.implementation.head.Head;
import tsp.headdb.implementation.head.LocalHead;
import tsp.nexuslib.inventory.Button;
import tsp.nexuslib.inventory.PagedPane;
import tsp.nexuslib.inventory.Pane;
import tsp.nexuslib.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

public class CommandMain extends HeadDBCommand implements CommandExecutor, TabCompleter {

    public CommandMain() {
        super("headdb", "headdb.command.open", HeadDB.getInstance().getCommandManager().getCommandsMap().values().stream()
                .map(HeadDBCommand::getName).collect(Collectors.toList()));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void handle(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof final Player player)) {
                this.getLocalization().sendConsoleMessage("noConsole");
                return;
            }

            if (!player.hasPermission(this.getPermission())) {
                this.getLocalization().sendMessage(sender, "noPermission");
                return;
            }
            this.getLocalization().sendMessage(player.getUniqueId(), "openDatabase");

            final Pane pane = new Pane(6, Utils.translateTitle(this.getLocalization().getMessage(player.getUniqueId(), "menu.main.title")
                    .orElse("&cHeadDB &7(" + HeadAPI.getTotalHeads() + ")"), HeadAPI.getTotalHeads(), "Main"));
            // Set category buttons
            for (final Category category : Category.VALUES) {
                pane.setButton(this.getInstance().getConfig().getInt("gui.main.category." + category.getName(), category.getDefaultSlot()),
                        new Button(category.getItem(player.getUniqueId()), e -> {
                            e.setCancelled(true);
                            if (e.isLeftClick()) {
                                Bukkit.dispatchCommand(e.getWhoClicked(), "hdb open " + category.getName());
                            } else if (e.isRightClick()) {
                                new AnvilGUI.Builder().onClick((slot, stateSnapshot) -> {
                                    try {
                                        final int page = Integer.parseInt(stateSnapshot.getText());
                                        // to be replaced with own version of anvil-gui
                                        final List<Head> heads = HeadAPI.getHeads(category);
                                        final PagedPane main = Utils.createPaged(player,
                                                Utils.translateTitle(this.getLocalization()
                                                        .getMessage(player.getUniqueId(), "menu.category.name").orElse(category.getName()),
                                                        heads.size(), category.getName()));
                                        Utils.addHeads(player, category, main, heads);
                                        main.selectPage(page);
                                        main.reRender();
                                        return Arrays.asList(AnvilGUI.ResponseAction.openInventory(main.getInventory()));
                                    } catch (final NumberFormatException nfe) {
                                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Invalid number!"));
                                    }
                                }).text("Query")
                                        .title(StringUtils.colorize(this.getLocalization()
                                                .getMessage(player.getUniqueId(), "menu.main.category.page.name").orElse("Enter page")))
                                        .plugin(this.getInstance()).open(player);
                            }
                        }));
            }

            // Set meta buttons
            // favorites
            pane.setButton(this.getInstance().getConfig().getInt("gui.main.meta.favorites.slot"),
                    new Button(Utils.getItemFromConfig("gui.main.meta.favorites.item", Material.BOOK), e -> {
                        e.setCancelled(true);
                        if (!player.hasPermission("headdb.favorites")) {
                            HeadDB.getInstance().getLocalization().sendMessage(player, "noAccessFavorites");
                            return;
                        }

                        Utils.openFavoritesMenu(player);
                    }));

            // search
            pane.setButton(this.getInstance().getConfig().getInt("gui.main.meta.search.slot"),
                    new Button(Utils.getItemFromConfig("gui.main.meta.search.item", Material.DARK_OAK_SIGN), e -> {
                        e.setCancelled(true);
                        new AnvilGUI.Builder().onClick((slot, stateSnapshot) -> {
                            // Copied from CommandSearch
                            final List<Head> heads = new ArrayList<>();
                            final List<Head> headList = HeadAPI.getHeads();
                            if (stateSnapshot.getText().length() > 3) {
                                if (stateSnapshot.getText().startsWith("id:")) {
                                    try {
                                        HeadAPI.getHeadById(Integer.parseInt(stateSnapshot.getText().substring(3))).ifPresent(heads::add);
                                    } catch (final NumberFormatException ignored) {
                                    }
                                } else if (stateSnapshot.getText().startsWith("tg:")) {
                                    heads.addAll(headList.stream()
                                            .filter(head -> Utils.matches(head.getTags(), stateSnapshot.getText().substring(3))).toList());
                                } else {
                                    // no query prefix
                                    heads.addAll(headList.stream().filter(head -> Utils.matches(head.getName(), stateSnapshot.getText()))
                                            .toList());
                                }
                            } else {
                                // query is <=3, no point in looking for prefixes
                                heads.addAll(
                                        headList.stream().filter(head -> Utils.matches(head.getName(), stateSnapshot.getText())).toList());
                            }

                            final PagedPane main = Utils.createPaged(player,
                                    Utils.translateTitle(this.getLocalization().getMessage(player.getUniqueId(), "menu.search.name")
                                            .orElse("&cHeadDB - &eSearch Results"), heads.size(), "None", stateSnapshot.getText()));
                            Utils.addHeads(player, null, main, heads);
                            main.reRender();
                            return Arrays.asList(ResponseAction.openInventory(main.getInventory()));

                        }).title(StringUtils
                                .colorize(this.getLocalization().getMessage(player.getUniqueId(), "menu.main.search.name").orElse("Search")))
                                .text("Query").plugin(this.getInstance()).open(player);
                    }));

            // local
            if (this.getInstance().getConfig().getBoolean("localHeads")) {
                pane.setButton(this.getInstance().getConfig().getInt("gui.main.meta.local.slot"),
                        new Button(Utils.getItemFromConfig("gui.main.meta.local.item", Material.COMPASS), e -> {
                            final Set<LocalHead> localHeads = HeadAPI.getLocalHeads();
                            final PagedPane localPane = Utils.createPaged(player, Utils.translateTitle(
                                    this.getLocalization().getMessage(player.getUniqueId(), "menu.main.local.name").orElse("Local Heads"),
                                    localHeads.size(), "Local"));
                            for (final LocalHead head : localHeads) {
                                localPane.addButton(new Button(head.getItem(), le -> {
                                    if (le.isLeftClick()) {
                                        final ItemStack localItem = head.getItem();
                                        if (le.isShiftClick()) {
                                            localItem.setAmount(64);
                                        }

                                        player.getInventory().addItem(localItem);
                                    }
                                }));
                            }

                            localPane.open(player);
                        }));
            }

            // Fill
            Utils.fill(pane, Utils.getItemFromConfig("gui.main.fill", Material.BLACK_STAINED_GLASS_PANE));

            pane.open(player);
            return;
        }

        this.getInstance().getCommandManager().getCommand(args[0]).ifPresentOrElse(command -> {
            if (sender instanceof final Player player && !player.hasPermission(command.getPermission())) {
                this.getLocalization().sendMessage(player.getUniqueId(), "noPermission");
                return;
            }

            command.handle(sender, args);
        }, () -> this.getLocalization().sendMessage(sender, "invalidSubCommand"));
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        this.handle(sender, args);
        return true;
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            return new ArrayList<>(this.getCompletions());
        } else {
            final Optional<SubCommand> sub = this.getInstance().getCommandManager().getCommand(args[0]);
            if (sub.isPresent()) {
                return new ArrayList<>(sub.get().getCompletions());
            }
        }
        return null;
    }

}
