package github.dluckycompany.clawkins.ui;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Standalone summary overlay for a selected Clawkin.
 *
 * Data-driven: all profile/skill text should come from the loaded Clawkin and
 * BattleSkill metadata when present.
 */
public class SummaryScreen implements InputProcessor {

    private static final Color DARK_BG = Color.valueOf("#1F1A13");
    private static final Color PANEL_BG = Color.valueOf("#C19253");
    private static final Color CARD_BG = Color.valueOf("#E7DAC7");
    private static final Color TEXT = Color.valueOf("#1F1A13");
    private static final Color SUBTEXT = Color.valueOf("#3A3024");
    private static final Color SKILL_SELECTED = Color.valueOf("#F0DBA8");
    private static final Color SKILL_UNSELECTED = Color.valueOf("#D9CCB8");
    private static final Color BAR_TRACK = Color.valueOf("#6B655C");
    private static final Color HP_FILL = Color.valueOf("#4E9A67");
    private static final Color ATK_FILL = Color.valueOf("#CB7C50");
    private static final Color DEF_FILL = Color.valueOf("#5E86C2");
    private static final Color SPD_FILL = Color.valueOf("#A26BC9");
    private static final Color CARD_SOFT = Color.valueOf("#D9CCB8");
    private static final Color CARD_EMPHASIS = Color.valueOf("#EAD49A");

    private static final float LEFT_PANEL_WIDTH = 230f;
    private static final float FOOTER_HEIGHT = 50f;
    private static final float SKILL_GAP = 10f;
    private static final float MIN_SKILL_LIST_WIDTH = 190f;
    private static final float MAX_SKILL_LIST_WIDTH = 300f;
    private static final float MIN_SKILL_DETAIL_WIDTH = 170f;

    private final Stage stage;
    private final Clawkin clawkin;
    private final BitmapFont font;
    private final Runnable onBackPressed;

    private final Table rootTable;
    private final Table contentTable;
    private final Table footerTable;
    private final Label footerLabel;
    private final Label.LabelStyle h1Style;
    private final Label.LabelStyle h2Style;
    private final Label.LabelStyle bodyStyle;
    private final Label.LabelStyle subStyle;

    private Texture portraitTexture;
    private TextureRegionDrawable portraitDrawable;
    private InputMultiplexer inputMultiplexer;

    private Page page = Page.STATS;
    private int selectedSkillIndex;

    private enum Page {
        STATS,
        SKILLS
    }

    public SummaryScreen(Stage stage, Clawkin clawkin, BitmapFont font, Runnable onBackPressed) {
        this.stage = stage;
        this.clawkin = clawkin;
        this.font = font;
        this.onBackPressed = onBackPressed;

        this.rootTable = new Table();
        this.contentTable = new Table();
        this.footerTable = new Table();
        this.h1Style = new Label.LabelStyle(font, Color.valueOf("#2B2014"));
        this.h2Style = new Label.LabelStyle(font, Color.valueOf("#3A2B1B"));
        this.bodyStyle = new Label.LabelStyle(font, TEXT);
        this.subStyle = new Label.LabelStyle(font, SUBTEXT);
        this.footerLabel = new Label("", new Label.LabelStyle(font, TEXT));

        this.selectedSkillIndex = 0;

        buildLayout();
        setupInputProcessor();
    }

    private void buildLayout() {
        rootTable.clear();
        rootTable.setFillParent(true);
        rootTable.setBackground(new ColorDrawable(DARK_BG));
        rootTable.pad(16f);

        contentTable.clear();
        contentTable.setBackground(new ColorDrawable(PANEL_BG));
        contentTable.pad(12f);
        contentTable.top().left();

        Label screenHeading = new Label("CLAWKIN SUMMARY", h1Style);
        screenHeading.setFontScale(1.18f);
        contentTable.add(screenHeading).left().padBottom(8f).row();

        addDivider(contentTable);

        Table body = new Table();
        body.top().left();
        body.add(buildLeftPanel()).width(LEFT_PANEL_WIDTH).fillY().padRight(12f);
        body.add(buildRightPanel()).expand().fill();

        contentTable.add(body).expand().fill().padTop(8f);

        footerTable.clear();
        footerTable.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(CARD_BG, 12, 2));
        footerTable.pad(8f, 12f, 8f, 12f);

        int pageIndex = page == Page.STATS ? 1 : 2;
        String pageName = page == Page.STATS ? "STATS" : "SKILLS";
        String navText = "[A/D] Switch Page     [ESC] Back";
        if (page == Page.SKILLS) {
            navText = "[A/D] Switch Page     [UP/DOWN] Select Skill     [ESC] Back";
        }
        footerLabel.setText(navText + "     [" + pageIndex + "/2] " + pageName);
        footerLabel.setFontScale(0.82f);
        footerLabel.setAlignment(Align.left);

        footerTable.add(footerLabel).left().expandX().fillX();

        rootTable.add(contentTable).expand().fill().row();
        rootTable.add(footerTable).expandX().fillX().height(FOOTER_HEIGHT).padTop(10f);
    }

    private Table buildLeftPanel() {
        Table left = new Table();
        left.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(CARD_BG, 12, 2));
        left.pad(10f);
        left.top().left();
        left.setClip(true);

        Table portraitFrame = new Table();
        portraitFrame.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_BG, 10));
        portraitFrame.pad(6f);

        Image portrait = buildPortrait();
        if (portrait != null) {
            portraitFrame.add(portrait).size(120f, 120f).center();
        } else {
            Label missing = new Label("NO PORTRAIT", new Label.LabelStyle(font, TEXT));
            missing.setFontScale(0.8f);
            portraitFrame.add(missing).size(120f, 120f).center();
        }

        Label name = new Label(clawkin.getName(), h1Style);
        name.setFontScale(1.15f);

        Label animalBreed = new Label(getDisplaySpecies(), subStyle);
        animalBreed.setFontScale(0.82f);

        Label role = new Label("Role: " + getCoreRole(), subStyle);
        role.setFontScale(0.82f);

        Label title = new Label("Title: " + getCoreTitle(), subStyle);
        title.setFontScale(0.78f);

        Table identityCard = new Table();
        identityCard.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_SOFT, 8));
        identityCard.pad(8f);
        identityCard.top().left();

        Label coreTitle = new Label("CORE IDENTITY", h2Style);
        coreTitle.setFontScale(0.8f);
        Label identityText = new Label("- " + getCoreRole() + "\n- " + getDisplaySpecies() + "\n- " + getCoreTitle(), subStyle);
        identityText.setWrap(true);
        identityText.setFontScale(0.74f);

        identityCard.add(coreTitle).left().padBottom(5f).row();
        identityCard.add(identityText).width(188f).left();

        Table overviewCard = new Table();
        overviewCard.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_EMPHASIS, 8));
        overviewCard.pad(8f);
        overviewCard.top().left();

        Label overviewTitle = new Label("CHARACTER OVERVIEW", h2Style);
        overviewTitle.setFontScale(0.8f);

        Label overviewText = new Label(getOverviewText(), bodyStyle);
        overviewText.setWrap(true);
        overviewText.setFontScale(0.74f);

        overviewCard.add(overviewTitle).left().padBottom(5f).row();
        overviewCard.add(overviewText).width(188f).left();

        left.add(portraitFrame).padBottom(10f).row();
        left.add(name).left().padBottom(4f).row();
        left.add(animalBreed).left().padBottom(4f).row();
        left.add(role).left().padBottom(3f).row();
        left.add(title).left().padBottom(8f).row();
        left.add(identityCard).growX().padBottom(10f).row();
        left.add(overviewCard).growX().expandY().fillY().top();

        return left;
    }

    private Table buildRightPanel() {
        return page == Page.STATS ? buildStatsPanel() : buildSkillsPanel();
    }

    private Table buildStatsPanel() {
        Table right = new Table();
        right.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(CARD_BG, 12, 2));
        right.pad(12f);
        right.top().left();
        right.setClip(true);

        Label title = new Label("BASE STATISTICS", h1Style);
        title.setFontScale(1.1f);
        right.add(title).left().padBottom(4f).row();

        Label subTitle = new Label("Profile", h2Style);
        subTitle.setFontScale(0.78f);
        right.add(subTitle).left().padBottom(8f).row();

        addDivider(right);

        Table scrollerContent = new Table();
        scrollerContent.top().left();
        scrollerContent.defaults().growX();

        Table statBox = new Table();
        statBox.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_SOFT, 8));
        statBox.pad(8f);
        statBox.top().left();
        statBox.defaults().growX().padBottom(8f);

        Clawkin.SummaryProfile profile = clawkin.getSummaryProfile();
        int maxStat = Math.max(1, Math.max(profile.getProfileHp(),
            Math.max(profile.getProfileAttack(), Math.max(profile.getProfileDefense(), profile.getProfileSpeed()))));

        statBox.add(new StatBar(font, "HP", profile.getProfileHp(), maxStat, HP_FILL)).row();
        statBox.add(new StatBar(font, "ATK", profile.getProfileAttack(), maxStat, ATK_FILL)).row();
        statBox.add(new StatBar(font, "DEF", profile.getProfileDefense(), maxStat, DEF_FILL)).row();
        statBox.add(new StatBar(font, "SPEED", profile.getProfileSpeed(), maxStat, SPD_FILL)).row();

        scrollerContent.add(statBox).growX().padTop(10f).row();

        Table profileSummary = new Table();
        profileSummary.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_EMPHASIS, 8));
        profileSummary.pad(8f);
        profileSummary.top().left();

        Label summaryTitle = new Label("OVERVIEW", h2Style);
        summaryTitle.setFontScale(0.82f);
        Label summaryText = new Label(getOverviewText(), bodyStyle);
        summaryText.setWrap(true);
        summaryText.setFontScale(0.76f);

        profileSummary.add(summaryTitle).left().padBottom(5f).row();
        profileSummary.add(summaryText).width(520f).left();

        scrollerContent.add(profileSummary).growX().padTop(10f);

        ScrollPane statsScroll = buildScrollPane(scrollerContent);
        right.add(statsScroll).expand().fill().padTop(8f);

        return right;
    }

    private Table buildSkillsPanel() {
        Table right = new Table();
        right.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(CARD_BG, 12, 2));
        right.pad(12f);
        right.top().left();
        right.setClip(true);

        Label title = new Label("SKILLS", h1Style);
        title.setFontScale(1.1f);
        right.add(title).left().padBottom(8f).row();

        addDivider(right);

        List<BattleSkill> skills = clawkin.getSkills();
        if (skills.isEmpty()) {
            Label none = new Label("No skills configured", new Label.LabelStyle(font, TEXT));
            none.setFontScale(0.9f);
            right.add(none).left().padTop(10f);
            return right;
        }

        selectedSkillIndex = Math.max(0, Math.min(selectedSkillIndex, skills.size() - 1));

        float splitContentWidth = computeSkillSplitContentWidth();
        float listWidth = Math.max(MIN_SKILL_LIST_WIDTH,
            Math.min(MAX_SKILL_LIST_WIDTH, splitContentWidth * 0.52f));
        float detailWidth = Math.max(MIN_SKILL_DETAIL_WIDTH, splitContentWidth - listWidth - SKILL_GAP);
        float rowDescWidth = Math.max(110f, listWidth - 40f);
        float detailTextWidth = Math.max(120f, detailWidth - 24f);

        Table listPanel = new Table();
        listPanel.top().left();
        listPanel.defaults().growX().height(98f).padBottom(8f);

        for (int i = 0; i < skills.size(); i++) {
            BattleSkill skill = skills.get(i);
            SkillRow row = new SkillRow(
                font,
                skill.getName(),
                buildTypeText(skill),
                skill.getTurnCooldown(),
                i == selectedSkillIndex,
                rowDescWidth);
            final int idx = i;
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedSkillIndex = idx;
                    buildLayout();
                }
            });
            listPanel.add(row).row();
        }

        ScrollPane listScroll = buildScrollPane(listPanel);

        Table detailPanel = buildSkillDetailPanel(skills.get(selectedSkillIndex), detailTextWidth);
        ScrollPane detailScroll = buildScrollPane(detailPanel);

        Table split = new Table();
        split.top().left();
        split.setClip(true);
        split.add(listScroll).width(listWidth).growY().padRight(SKILL_GAP);
        split.add(detailScroll).width(detailWidth).growY();

        right.add(split).expand().fill().padTop(10f);
        return right;
    }

    private float computeSkillSplitContentWidth() {
        float worldWidth = stage.getViewport().getWorldWidth();
        float totalHorizontalPadding = (16f * 2f) + (12f * 2f) + LEFT_PANEL_WIDTH + 12f + (12f * 2f);
        float available = worldWidth - totalHorizontalPadding;
        return Math.max(MIN_SKILL_LIST_WIDTH + MIN_SKILL_DETAIL_WIDTH + SKILL_GAP, available);
    }

    private Table buildSkillDetailPanel(BattleSkill skill, float detailTextWidth) {
        Table detail = new Table();
        detail.setBackground(RoundedPanelDrawable.createRoundedPanel(SKILL_SELECTED, 10));
        detail.pad(10f);
        detail.top().left();

        Label name = new Label(skill.getName(), h2Style);
        name.setFontScale(1.0f);

        String description = sanitizeSkillDescription(skill.getSummaryDescription());
        String effects = buildEffectText(skill);
        String type = buildTypeText(skill);
        String cooldown = skill.getTurnCooldown() + " Turns";

        Table nameCard = new Table();
        nameCard.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_SOFT, 8));
        nameCard.pad(8f);
        nameCard.top().left();
        nameCard.add(name).left();

        Table divider = new Table();
        divider.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));

        Table descCard = new Table();
        descCard.setBackground(RoundedPanelDrawable.createRoundedPanel(CARD_EMPHASIS, 8));
        descCard.pad(8f);
        descCard.top().left();
        descCard.defaults().left().growX();

        Label cooldownLabel = new Label("COOLDOWN", h2Style);
        cooldownLabel.setFontScale(0.95f);
        Label cooldownValue = new Label(cooldown, bodyStyle);
        cooldownValue.setFontScale(0.82f);
        cooldownValue.setWrap(true);

        Label typeLabel = new Label("TYPE", h2Style);
        typeLabel.setFontScale(0.95f);
        Label typeValue = new Label(type, bodyStyle);
        typeValue.setFontScale(0.82f);
        typeValue.setWrap(true);

        Label descriptionLabel = new Label("DESCRIPTION", h2Style);
        descriptionLabel.setFontScale(0.95f);
        Label descriptionValue = new Label(description.isBlank() ? "No description provided." : description, bodyStyle);
        descriptionValue.setWrap(true);
        descriptionValue.setFontScale(0.82f);

        Label effectsLabel = new Label("EFFECTS", h2Style);
        effectsLabel.setFontScale(0.95f);
        Label effectsValue = new Label(effects, bodyStyle);
        effectsValue.setWrap(true);
        effectsValue.setFontScale(0.82f);

        Table sectionDivider1 = new Table();
        sectionDivider1.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));
        Table sectionDivider2 = new Table();
        sectionDivider2.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));
        Table sectionDivider3 = new Table();
        sectionDivider3.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));

        descCard.add(cooldownLabel).padBottom(2f).row();
        descCard.add(cooldownValue).width(detailTextWidth).padBottom(6f).row();
        descCard.add(sectionDivider1).height(1f).padBottom(6f).row();

        descCard.add(typeLabel).padBottom(2f).row();
        descCard.add(typeValue).width(detailTextWidth).padBottom(6f).row();
        descCard.add(sectionDivider2).height(1f).padBottom(6f).row();

        descCard.add(descriptionLabel).padBottom(2f).row();
        descCard.add(descriptionValue).width(detailTextWidth).padBottom(6f).row();
        descCard.add(sectionDivider3).height(1f).padBottom(6f).row();

        descCard.add(effectsLabel).padBottom(2f).row();
        descCard.add(effectsValue).width(detailTextWidth).left();

        detail.add(nameCard).growX().left().padBottom(8f).row();
        detail.add(divider).growX().height(2f).padBottom(8f).row();
        detail.add(descCard).growX().left();

        return detail;
    }

    private ScrollPane buildScrollPane(Actor content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, true);
        pane.setScrollbarsOnTop(true);
        return pane;
    }

    private void addDivider(Table table) {
        Table line = new Table();
        line.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));
        table.add(line).growX().height(2f).row();
    }

    private Image buildPortrait() {
        if (portraitDrawable != null) {
            Image image = new Image(portraitDrawable);
            image.setScaling(Scaling.fit);
            return image;
        }

        String imagePath = clawkin.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalized = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
        try {
            portraitTexture = new Texture(Gdx.files.internal(normalized));
            portraitDrawable = new TextureRegionDrawable(portraitTexture);
            Image image = new Image(portraitDrawable);
            image.setScaling(Scaling.fit);
            return image;
        } catch (Exception e) {
            Gdx.app.log("SummaryScreen", "Failed to load portrait: " + normalized + " -> " + e.getMessage());
            return null;
        }
    }

    private void setupInputProcessor() {
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    public Table getRootTable() {
        return rootTable;
    }

    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.A, Input.Keys.D, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                togglePage();
                return true;
            }
            case Input.Keys.UP -> {
                if (page == Page.SKILLS) {
                    moveSkillSelection(-1);
                    return true;
                }
                return false;
            }
            case Input.Keys.DOWN -> {
                if (page == Page.SKILLS) {
                    moveSkillSelection(1);
                    return true;
                }
                return false;
            }
            case Input.Keys.ESCAPE -> {
                if (onBackPressed != null) {
                    onBackPressed.run();
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void moveSkillSelection(int delta) {
        List<BattleSkill> skills = clawkin.getSkills();
        if (skills.isEmpty()) {
            selectedSkillIndex = 0;
            return;
        }
        int size = skills.size();
        selectedSkillIndex = (selectedSkillIndex + delta + size) % size;
        buildLayout();
    }

    private void togglePage() {
        page = (page == Page.STATS) ? Page.SKILLS : Page.STATS;
        buildLayout();
    }

    private String getDisplaySpecies() {
        String species = clawkin.getSummaryProfile().getSpecies();
        return species.isBlank() ? clawkin.getName() : species;
    }

    private String getCoreRole() {
        String role = clawkin.getSummaryProfile().getRole();
        return role.isBlank() ? "Unspecified" : role;
    }

    private String getCoreTitle() {
        String title = clawkin.getSummaryProfile().getTitle();
        return title.isBlank() ? "Unspecified" : title;
    }

    private String getOverviewText() {
        return defaultIfBlank(clawkin.getSummaryProfile().getOverview(), "No overview provided.");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildEffectText(BattleSkill skill) {
        if (!skill.getSummaryEffectText().isBlank()) {
            return skill.getSummaryEffectText();
        }
        return buildDataFallback(skill);
    }

    private String buildTypeText(BattleSkill skill) {
        String raw = skill.getSummaryDescription();
        if (raw != null) {
            int markerIndex = raw.toLowerCase().indexOf("| type:");
            if (markerIndex >= 0) {
                int start = markerIndex + "| type:".length();
                String afterType = raw.substring(start).trim();
                int end = afterType.indexOf('.');
                String typeValue = end >= 0 ? afterType.substring(0, end).trim() : afterType.trim();
                return typeValue.isBlank() ? humanizeEffectType(skill) : typeValue;
            }
        }
        return humanizeEffectType(skill);
    }

    private String humanizeEffectType(BattleSkill skill) {
        return switch (skill.getEffectType()) {
            case DAMAGE -> "Physical";
            case HEAL -> "Status";
            case ATTACK -> "Status";
            case DEFENSE -> "Status";
        };
    }

    private String extractScalingSource(BattleSkill skill) {
        if (!skill.getSummaryScalingText().isBlank()) {
            return skill.getSummaryScalingText();
        }
        String scale = skill.getEffectStatScale();
        if (scale == null || scale.isBlank()) {
            return "none";
        }
        String lower = scale.toLowerCase();
        if (lower.contains("def")) {
            return "DEF";
        }
        if (lower.contains("spd") || lower.contains("speed")) {
            return "SPD";
        }
        if (lower.contains("atk") || lower.contains("attack")) {
            return "ATK";
        }
        return scale;
    }

    private String buildDataFallback(BattleSkill skill) {
        return "Type=" + skill.getEffectType()
            + ", Base=" + skill.getEffectBaseStat()
            + ", Scale=" + extractScalingSource(skill)
            + ", Duration=" + Math.max(0, skill.getEffectDurationTurns()) + " turn(s)";
    }

    private String sanitizeSkillDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return "No description provided.";
        }

        String cleaned = raw.replace('\n', ' ').trim();

        // Remove any "Unlocked at ... | Type: ..." preamble and keep only gameplay description.
        cleaned = cleaned.replaceAll("(?i)^\\s*unlocked\\s+at[^|]*\\|\\s*type\\s*:[^.]*\\.\\s*", "");
        cleaned = cleaned.replaceAll("(?i)^\\s*[^|]*\\|\\s*type\\s*:[^.]*\\.\\s*", "");
        cleaned = cleaned.replaceAll("(?i)^\\s*type\\s*:[^.]*\\.\\s*", "");

        int visualAudioIdx = cleaned.toLowerCase().indexOf("visual/audio:");
        if (visualAudioIdx >= 0) {
            cleaned = cleaned.substring(0, visualAudioIdx).trim();
        }

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned.isBlank() ? "No description provided." : cleaned;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    public void dispose() {
        if (rootTable != null) {
            rootTable.remove();
            rootTable.clearActions();
        }
        if (portraitTexture != null) {
            portraitTexture.dispose();
            portraitTexture = null;
            portraitDrawable = null;
        }
    }

    private static class SkillRow extends Table {
        SkillRow(
            BitmapFont font,
            String skillName,
            String skillType,
            int cooldown,
            boolean selected,
            float descWidth
        ) {
            setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
                selected ? SKILL_SELECTED : SKILL_UNSELECTED,
                10,
                2
            ));
            pad(10f);
            left().top();

            Label.LabelStyle primary = new Label.LabelStyle(font, TEXT);
            Label.LabelStyle secondary = new Label.LabelStyle(font, SUBTEXT);

            Label name = new Label(skillName, primary);
            name.setFontScale(1.0f);

            Table accent = new Table();
            accent.setBackground(new ColorDrawable(Color.valueOf("#2A2219")));

            Label type = new Label("Type: " + skillType, secondary);
            type.setFontScale(0.82f);

            Label cd = new Label("Cooldown: " + cooldown, secondary);
            cd.setFontScale(0.82f);

            add(name).left().row();
            add(accent).width(descWidth).height(2f).left().padTop(4f).padBottom(4f).row();
            add(type).left().padTop(1f).row();
            add(cd).left().padTop(1f);
        }
    }

    private static class StatBar extends Table {
        StatBar(BitmapFont font, String label, int value, int maxValue, Color fillColor) {
            left();
            Label.LabelStyle style = new Label.LabelStyle(font, TEXT);

            Label name = new Label(label, style);
            name.setFontScale(1.0f);

            Label number = new Label(String.valueOf(value), style);
            number.setFontScale(1.0f);

            float ratio = maxValue <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) value / (float) maxValue));
            float totalWidth = 300f;
            float fillWidth = Math.max(2f, totalWidth * ratio);

            Table track = new Table();
            track.setBackground(RoundedPanelDrawable.createRoundedPanel(BAR_TRACK, 6));
            track.left();
            Table fill = new Table();
            fill.setBackground(RoundedPanelDrawable.createRoundedPanel(fillColor, 6));
            track.add(fill).width(fillWidth).height(16f).left();

            add(name).width(66f).left().padRight(10f);
            add(track).width(totalWidth).height(16f).left().padRight(10f);
            add(number).width(56f).left();
        }
    }
}
