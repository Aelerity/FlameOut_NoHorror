package flame.unit;

/** Abilities are invoked on the possessed entity; its Player controller stays in place. */
public interface PlayerAbilityUnit{
    String[] playerAbilityNames();
    int selectedPlayerAbility();
    void selectPlayerAbility(int index);
    boolean usePlayerAbility();
    void cancelPlayerAbility();
    String playerAbilityStatus();

    /** Whether the extra native primary weapon should run this tick. */
    default boolean nativePlayerWeapon(){ return true; }
}
