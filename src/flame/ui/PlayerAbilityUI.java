package flame.ui;

import arc.*;
import arc.input.*;
import arc.input.KeyBind.*;
import arc.scene.ui.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import flame.unit.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/** A compact HUD selector plus rebindable controls usable while aiming in the world. */
public class PlayerAbilityUI{
    public static final KeyBind next = KeyBind.add("flameout-next-ability", KeyCode.f7, "flameout"),
        use = KeyBind.add("flameout-use-ability", KeyCode.f8),
        cancel = KeyBind.add("flameout-cancel-ability", KeyCode.f9);
    private static Unit bound;

    private static PlayerAbilityUnit current(){
        return Vars.player != null && Vars.player.unit() instanceof PlayerAbilityUnit abilities &&
            Vars.player.unit().isPlayer() ? abilities : null;
    }

    public static void load(){
        Table root = new Table();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        root.top().left().marginTop(110f).marginLeft(8f);
        Table panel = new Table(Tex.button);
        root.add(panel).width(330f);
        root.visible(() -> Vars.state.isGame() && Vars.ui.hudfrag.shown && current() != null);
        panel.update(() -> {
            Unit unit = Vars.player == null ? null : Vars.player.unit();
            if(unit == bound) return;
            bound = unit;
            panel.clearChildren();
            PlayerAbilityUnit abilities = current();
            if(abilities == null) return;
            panel.defaults().pad(4f).growX();
            panel.add("Flame Out abilities").row();
            panel.button("", () -> {
                if(current() != abilities) return;
                BaseDialog dialog = new BaseDialog("Select ability");
                dialog.cont.pane(list -> {
                    String[] names = abilities.playerAbilityNames();
                    for(int i = 0; i < names.length; i++){
                        final int index = i;
                        list.button(names[i], () -> {
                            if(current() == abilities) abilities.selectPlayerAbility(index);
                            dialog.hide();
                        }).width(350f).height(42f).row();
                    }
                }).maxHeight(500f);
                dialog.addCloseButton();
                dialog.show();
            }).update(button -> {
                if(current() == abilities) button.setText(abilities.playerAbilityNames()[abilities.selectedPlayerAbility()]);
            }).row();
            panel.table(buttons -> {
                buttons.defaults().growX().height(36f);
                buttons.button("Use", () -> { if(current() == abilities) abilities.usePlayerAbility(); });
                buttons.button("Cancel", () -> { if(current() == abilities) abilities.cancelPlayerAbility(); });
            }).row();
            panel.label(() -> current() == abilities ? abilities.playerAbilityStatus() : "").wrap().row();
            panel.add("Defaults: F7 next / F8 use / F9 cancel\nRebind in Settings > Controls").color(mindustry.graphics.Pal.lightishGray).wrap();
        });
        Vars.ui.hudGroup.addChild(root);
        Events.run(Trigger.update, () -> {
            PlayerAbilityUnit abilities = current();
            if(abilities != null) PlayerTargets.captureCursor(Vars.player.unit());
            if(abilities == null || !Vars.state.isGame() || Vars.state.isPaused() || Core.scene.hasDialog() || Core.scene.hasKeyboard()) return;
            if(Core.input.keyTap(next)) abilities.selectPlayerAbility((abilities.selectedPlayerAbility() + 1) % abilities.playerAbilityNames().length);
            if(Core.input.keyTap(use)) abilities.usePlayerAbility();
            if(Core.input.keyTap(cancel)) abilities.cancelPlayerAbility();
        });
    }
}
