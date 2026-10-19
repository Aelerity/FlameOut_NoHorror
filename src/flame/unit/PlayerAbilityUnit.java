package flame.unit;

/** Internal original-pattern hooks; automatic combat keeps the Player controller in place. */
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
