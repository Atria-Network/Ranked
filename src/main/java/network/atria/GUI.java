package network.atria;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import network.atria.Pick.PickGUI;
import network.atria.Veto.VetoGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class GUI {

  private final Inventory gui;
  private final Map<Integer, Action> actions;

  public GUI(int size, String title) {
    gui = Bukkit.createInventory(null, size, title);
    actions = Maps.newHashMap();
  }

  public void setItem(int slot, ItemStack item) {
    gui.setItem(slot, item);
  }

  public void setItem(int slot, ItemStack item, Action action) {
    setItem(slot, item);
    actions.put(slot, action);
  }

  public void open(Player player) {
    player.openInventory(gui);
  }

  public Inventory getGUI() {
    return gui;
  }

  public Map<Integer, Action> getActions() {
    return actions;
  }

  public interface Action {
    void click(Player player);
  }

  public static class GUIManager {

    private final Set<GUI> guiList;
    private final PickGUI pickGUI;
    private final VetoGUI vetoGUI;

    public GUIManager() {
      pickGUI = new PickGUI();
      vetoGUI = new VetoGUI();
      this.guiList = Sets.newHashSet(pickGUI, vetoGUI);
    }

    public Optional<GUI> getGUI(String name) {
      return this.guiList.stream()
          .filter(x -> x.getGUI().getTitle().equalsIgnoreCase(name))
          .findFirst();
    }

    public PickGUI getPickGUI() {
      return pickGUI;
    }

    public VetoGUI getVetoGUI() {
      return vetoGUI;
    }
  }

  public static class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player)) {
        return;
      }

      Ranked.get()
          .getGUIManager()
          .getGUI(event.getView().getTitle())
          .ifPresent(
              gui -> {
                Action action = gui.getActions().get(event.getSlot());

                if (action != null) action.click(event.getActor());
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
              });
    }
  }
}
