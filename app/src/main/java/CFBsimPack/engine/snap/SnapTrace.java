package CFBsimPack.engine.snap;

import CFBsimPack.engine.ConceptFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Structured snap diagnostics for coach "why" UI.
 * Built at resolve time from protection / timeline / gap / INT participants.
 */
public final class SnapTrace {
    public final ConceptFamily family;
    public final List<String> summaryBullets;

    // Pass
    public final double pressureSec;
    public final boolean hotForced;
    public final String freeRusherName;
    public final String decision;
    public final String targetName;
    public final String routeName;
    public final double separation;
    public final double throwTimeSec;
    public final String coverageMode;
    public final String shell;
    public final double completionChance;
    public final double completionRoll;
    public final boolean catchMade;
    public final String intSource;
    public final String yardsSampleNotes;

    // Run
    public final String gapUsed;
    public final boolean cutback;
    public final double crease;
    public final boolean explosive;
    public final String carrierName;
    public final String firstLevelName;
    public final String secondLevelName;

    // Credited defenders (aligned with DefStatAttribution)
    public final String sackerName;
    public final String tacklerName;
    public final String coverageName;
    public final String tipperName;

    private SnapTrace(Builder b) {
        this.family = b.family != null ? b.family : ConceptFamily.SPECIAL;
        this.summaryBullets = Collections.unmodifiableList(new ArrayList<>(b.bullets));
        this.pressureSec = b.pressureSec;
        this.hotForced = b.hotForced;
        this.freeRusherName = nullToEmpty(b.freeRusherName);
        this.decision = nullToEmpty(b.decision);
        this.targetName = nullToEmpty(b.targetName);
        this.routeName = nullToEmpty(b.routeName);
        this.separation = b.separation;
        this.throwTimeSec = b.throwTimeSec;
        this.coverageMode = nullToEmpty(b.coverageMode);
        this.shell = nullToEmpty(b.shell);
        this.completionChance = b.completionChance;
        this.completionRoll = b.completionRoll;
        this.catchMade = b.catchMade;
        this.intSource = nullToEmpty(b.intSource);
        this.yardsSampleNotes = nullToEmpty(b.yardsSampleNotes);
        this.gapUsed = nullToEmpty(b.gapUsed);
        this.cutback = b.cutback;
        this.crease = b.crease;
        this.explosive = b.explosive;
        this.carrierName = nullToEmpty(b.carrierName);
        this.firstLevelName = nullToEmpty(b.firstLevelName);
        this.secondLevelName = nullToEmpty(b.secondLevelName);
        this.sackerName = nullToEmpty(b.sackerName);
        this.tacklerName = nullToEmpty(b.tacklerName);
        this.coverageName = nullToEmpty(b.coverageName);
        this.tipperName = nullToEmpty(b.tipperName);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    public static Builder builder(ConceptFamily family) {
        return new Builder(family);
    }

    public static final class Builder {
        private final ConceptFamily family;
        private final List<String> bullets = new ArrayList<>();
        private double pressureSec;
        private boolean hotForced;
        private String freeRusherName;
        private String decision;
        private String targetName;
        private String routeName;
        private double separation;
        private double throwTimeSec;
        private String coverageMode;
        private String shell;
        private double completionChance = -1;
        private double completionRoll = -1;
        private boolean catchMade;
        private String intSource;
        private String yardsSampleNotes;
        private String gapUsed;
        private boolean cutback;
        private double crease;
        private boolean explosive;
        private String carrierName;
        private String firstLevelName;
        private String secondLevelName;
        private String sackerName;
        private String tacklerName;
        private String coverageName;
        private String tipperName;

        private Builder(ConceptFamily family) {
            this.family = family;
        }

        public Builder bullet(String line) {
            if (line != null && !line.isEmpty() && bullets.size() < 4) {
                bullets.add(line);
            }
            return this;
        }

        public Builder protection(ProtectionResult p) {
            if (p == null) return this;
            pressureSec = p.earliestPressureSec;
            hotForced = p.hotForced;
            if (p.freeRusher != null) freeRusherName = p.freeRusher.name;
            return this;
        }

        public Builder throwWindow(ThrowWindow w) {
            if (w == null) return this;
            decision = w.decision != null ? w.decision.name() : "";
            throwTimeSec = w.throwTimeSec;
            separation = w.separation;
            if (w.target != null) targetName = w.target.name;
            if (w.route != null && w.route.route != null) {
                routeName = w.route.route.name();
            }
            if (w.coverage != null) {
                if (w.coverage.defender != null) coverageName = w.coverage.defender.name;
                if (w.coverage.mode != null) coverageMode = w.coverage.mode.name();
            }
            return this;
        }

        public Builder shell(SafetyHelp.Shell s) {
            if (s != null) shell = s.name();
            return this;
        }

        public Builder completion(double chance, double roll, boolean made) {
            completionChance = chance;
            completionRoll = roll;
            catchMade = made;
            return this;
        }

        public Builder intSource(IntResolver.Source source) {
            if (source != null && source != IntResolver.Source.NONE) {
                intSource = source.name();
            }
            return this;
        }

        public Builder tipper(String name) {
            tipperName = name;
            return this;
        }

        public Builder sacker(String name) {
            sackerName = name;
            return this;
        }

        public Builder tackler(String name) {
            tacklerName = name;
            return this;
        }

        public Builder yardsNotes(String notes) {
            yardsSampleNotes = notes;
            return this;
        }

        public Builder runGap(RunGapResult gap, String carrier) {
            if (gap == null) return this;
            if (gap.gapUsed != null) gapUsed = gap.gapUsed.name();
            cutback = gap.cutback;
            crease = gap.crease;
            explosive = gap.explosive;
            carrierName = carrier;
            if (gap.firstLevel != null) firstLevelName = gap.firstLevel.name;
            if (gap.secondLevel != null) secondLevelName = gap.secondLevel.name;
            return this;
        }

        public SnapTrace build() {
            if (bullets.isEmpty()) {
                autoBullets();
            }
            return new SnapTrace(this);
        }

        private void autoBullets() {
            if (family == ConceptFamily.RUN
                    || (gapUsed != null && !gapUsed.isEmpty())) {
                String gap = gapUsed != null && !gapUsed.isEmpty()
                        ? gapUsed.replace('_', ' ')
                        : "gap";
                String creaseLabel = String.format(Locale.US, "Crease %.1f", crease);
                if (cutback) {
                    bullet("Cutback " + gap + " · " + creaseLabel);
                } else {
                    bullet(gap + " · " + creaseLabel);
                }
                if (explosive) bullet("Explosive burst");
                if (firstLevelName != null && !firstLevelName.isEmpty()) {
                    bullet("First level " + firstLevelName);
                }
                if (tacklerName != null && !tacklerName.isEmpty()) {
                    bullet("Tackled by " + tacklerName);
                }
                return;
            }

            if (decision != null && decision.equals("PRESSURE_OUT")) {
                String pressure = String.format(Locale.US, "Pressure at %.1fs", pressureSec);
                if (freeRusherName != null && !freeRusherName.isEmpty()) {
                    bullet(pressure + " · " + freeRusherName + " free");
                } else if (hotForced) {
                    bullet(pressure + " · hot forced");
                } else {
                    bullet(pressure);
                }
                if (sackerName != null && !sackerName.isEmpty()) {
                    bullet("Sacked by " + sackerName);
                }
                return;
            }

            if (intSource != null && !intSource.isEmpty()) {
                if (tipperName != null && !tipperName.isEmpty()) {
                    bullet("Tipped by " + tipperName);
                }
                bullet("INT source " + intSource.replace('_', ' '));
                if (separation > 0) {
                    bullet(String.format(Locale.US, "Sep %.1f", separation));
                }
                return;
            }

            if (targetName != null && !targetName.isEmpty()) {
                String route = routeName != null && !routeName.isEmpty()
                        ? routeName.replace('_', ' ')
                        : "route";
                bullet(targetName + " · " + route
                        + String.format(Locale.US, " · sep %.1f", separation));
            }
            if (coverageName != null && !coverageName.isEmpty()) {
                String mode = coverageMode != null && !coverageMode.isEmpty()
                        ? coverageMode : "COV";
                bullet("Covered by " + coverageName + " (" + mode + ")");
            }
            if (completionChance >= 0 && completionRoll >= 0) {
                bullet(String.format(Locale.US, "Catch %.0f%% vs roll %.0f%s",
                        completionChance, completionRoll, catchMade ? " · catch" : " · miss"));
            } else if (pressureSec > 0) {
                bullet(String.format(Locale.US, "Pocket to %.1fs", pressureSec));
            }
            if (shell != null && !shell.isEmpty() && bullets.size() < 4) {
                bullet("Shell " + shell.replace('_', ' '));
            }
            if (tacklerName != null && !tacklerName.isEmpty() && bullets.size() < 4) {
                bullet("Tackled by " + tacklerName);
            }
        }
    }
}
