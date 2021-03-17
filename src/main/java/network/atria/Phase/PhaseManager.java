package network.atria.Phase;

public class PhaseManager {

  private static Phase phase = Phase.IDLE;

  public static void setPhase(Phase phase) {
    PhaseManager.phase = phase;
  }

  public static boolean checkPhase(Phase phase) {
    return PhaseManager.phase == phase;
  }

  public static Phase getPhase() {
    return phase;
  }
}
