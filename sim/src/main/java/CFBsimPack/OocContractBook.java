package CFBsimPack;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * League-wide multi-year OOC contract ledger.
 */
public final class OocContractBook {

    private final League league;
    private final ArrayList<OocContract> contracts = new ArrayList<>();
    private int nextId = 1;
    /** Last breach summaries for UI (cleared when read). */
    private final ArrayList<String> recentBreachNotices = new ArrayList<>();
    /** In-memory IDs from the latest user Suggest deals batch (not persisted). */
    private final ArrayList<String> suggestedDealIds = new ArrayList<>();

    public OocContractBook(League league) {
        this.league = league;
    }

    public List<OocContract> all() {
        return new ArrayList<>(contracts);
    }

    public int getNextId() {
        return nextId;
    }

    public List<OocContract> forTeam(String abbr) {
        ArrayList<OocContract> out = new ArrayList<>();
        for (OocContract c : contracts) {
            if (c.involves(abbr)) {
                out.add(c);
            }
        }
        return out;
    }

    public List<OocContract> forTeamInYear(String abbr, int year) {
        ArrayList<OocContract> out = new ArrayList<>();
        for (OocContract c : contracts) {
            if (c.involves(abbr) && c.gameForYear(year) != null) {
                out.add(c);
            }
        }
        return out;
    }

    public OocContract findById(String id) {
        for (OocContract c : contracts) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public OocContract findContractBetween(String a, String b, int year) {
        for (OocContract c : contracts) {
            if (c.involves(a) && c.involves(b) && c.gameForYear(year) != null) {
                return c;
            }
        }
        return null;
    }

    public boolean alreadyContracted(Team a, Team b, int year) {
        return findContractBetween(a.abbr, b.abbr, year) != null;
    }

    public List<String> consumeBreachNotices() {
        ArrayList<String> out = new ArrayList<>(recentBreachNotices);
        recentBreachNotices.clear();
        return out;
    }

    public String quoteBuyGame(Team home, Team away) {
        int g = NilMoney.buyGameGuarantee(home.programProfile, away.programProfile);
        int bonus = NilMoney.buyGameWinBonus(g);
        return "Guarantee " + NilMoney.format(g)
                + " (you pay) · Away win bonus " + NilMoney.format(bonus);
    }

    public String quoteReceiveBuyGame(Team home, Team away) {
        int g = NilMoney.buyGameGuarantee(home.programProfile, away.programProfile);
        int bonus = NilMoney.buyGameWinBonus(g);
        return "Guarantee " + NilMoney.format(g)
                + " (you receive) · Win bonus " + NilMoney.format(bonus);
    }

    /**
     * True when {@code home} is far enough below {@code away} that hosting means
     * buying the visit. Only the weaker school may propose that deal.
     */
    public static boolean isAppearanceMatchup(Team home, Team away) {
        if (home == null || away == null) {
            return false;
        }
        int tierGap = home.programProfile.scheduleTier - away.programProfile.scheduleTier;
        return tierGap < -NilMoney.PEER_SERIES_TIER_GAP;
    }

    /**
     * Signs a one-year single-game contract with automatic home→away fee.
     * Appearance matchups (weaker host buying a bigger visitor) are rejected
     * because only the smaller school may initiate one.
     */
    public OocContract signSingleGame(Team home, Team away, int year) {
        return signSingleGame(home, away, year, false);
    }

    /**
     * Signs a one-year single-game contract with automatic home→away fee.
     *
     * @param softInitiated the weaker host is choosing to buy the bigger visitor
     */
    public OocContract signSingleGame(Team home, Team away, int year, boolean softInitiated) {
        if (home == null || away == null || home == away) {
            return null;
        }
        if (home.conference.equals(away.conference)) {
            return null;
        }
        if (alreadyContracted(home, away, year)) {
            return null;
        }
        if (isAppearanceMatchup(home, away) && !softInitiated) {
            return null;
        }
        int guarantee = NilMoney.singleGameGuarantee(home.programProfile, away.programProfile);
        int winBonus = 0;
        if (guarantee > 0) {
            if (home.recruitMoney < guarantee) {
                return null;
            }
            winBonus = NilMoney.buyGameWinBonus(guarantee);
        }
        ArrayList<OocContractGame> games = new ArrayList<>();
        games.add(new OocContractGame(year, home.abbr, away.abbr, guarantee, winBonus));
        int tierGap = home.programProfile.scheduleTier - away.programProfile.scheduleTier;
        boolean nearPeer = Math.abs(tierGap) <= NilMoney.PEER_SERIES_TIER_GAP;
        OocContract.Type type = nearPeer ? OocContract.Type.SINGLE : OocContract.Type.BUY;
        int buyout = NilMoney.oocCancelBuyout(type, guarantee, 1, 1);
        OocContract contract = new OocContract(
                "C" + (nextId++),
                home.abbr,
                away.abbr,
                year,
                1,
                type,
                year,
                buyout,
                games);
        contracts.add(contract);
        return contract;
    }

    /**
     * Signs a buy-game series: same home each year for {@code years} (1–3).
     */
    public OocContract signBuyGame(Team home, Team away, int startYear, int years) {
        if (home == null || away == null || home == away) {
            return null;
        }
        if (home.conference.equals(away.conference)) {
            return null;
        }
        years = Math.max(1, Math.min(3, years));
        int guarantee = NilMoney.buyGameGuarantee(home.programProfile, away.programProfile);
        if (home.recruitMoney < guarantee) {
            return null;
        }
        for (int y = 0; y < years; y++) {
            if (alreadyContracted(home, away, startYear + y)) {
                return null;
            }
        }
        ArrayList<OocContractGame> games = new ArrayList<>();
        int winBonus = NilMoney.buyGameWinBonus(guarantee);
        for (int y = 0; y < years; y++) {
            games.add(new OocContractGame(
                    startYear + y, home.abbr, away.abbr, guarantee, winBonus));
        }
        int buyout = NilMoney.oocCancelBuyout(
                OocContract.Type.BUY, guarantee * years, years, years);
        OocContract contract = new OocContract(
                "C" + (nextId++),
                home.abbr,
                away.abbr,
                startYear,
                years,
                OocContract.Type.BUY,
                startYear + years - 1,
                buyout,
                games);
        contracts.add(contract);
        return contract;
    }

    /**
     * Signs a 2-year home-and-home starting {@code startYear} with return the next year.
     */
    public OocContract signHomeAndHome(Team teamA, Team teamB, int startYear, boolean aHomesFirst) {
        return signHomeAndHome(teamA, teamB, startYear, startYear + 1, aHomesFirst);
    }

    /**
     * Signs a home-and-home with a deferred return year (1–6 years after start).
     */
    public OocContract signHomeAndHome(
            Team teamA, Team teamB, int startYear, int returnYear, boolean aHomesFirst) {
        return signHomeAndHome(teamA, teamB, startYear, returnYear, aHomesFirst, false);
    }

    /**
     * Signs a home-and-home with a deferred return year (1–6 years after start).
     * Peers swap home dates for free. Beyond the peer band the weaker school owes
     * an appearance fee on its home date, so only it may propose the series.
     *
     * @param softInitiated the weaker school is choosing to buy its home date
     */
    public OocContract signHomeAndHome(
            Team teamA,
            Team teamB,
            int startYear,
            int returnYear,
            boolean aHomesFirst,
            boolean softInitiated) {
        if (teamA == null || teamB == null || teamA == teamB) {
            return null;
        }
        if (teamA.conference.equals(teamB.conference)) {
            return null;
        }
        int minReturn = startYear + 1;
        int maxReturn = startYear + 6;
        if (returnYear < minReturn) {
            returnYear = minReturn;
        }
        if (returnYear > maxReturn) {
            returnYear = maxReturn;
        }
        if (alreadyContracted(teamA, teamB, startYear)
                || alreadyContracted(teamA, teamB, returnYear)) {
            return null;
        }
        int tierGap = Math.abs(
                teamA.programProfile.scheduleTier - teamB.programProfile.scheduleTier);
        if (tierGap > NilMoney.PEER_SERIES_TIER_GAP && !softInitiated) {
            return null;
        }
        Team firstHome = aHomesFirst ? teamA : teamB;
        Team firstAway = aHomesFirst ? teamB : teamA;
        int firstFee = NilMoney.homeAndHomeLegFee(
                firstHome.programProfile, firstAway.programProfile);
        int returnFee = NilMoney.homeAndHomeLegFee(
                firstAway.programProfile, firstHome.programProfile);
        if (firstFee > 0 && firstHome.recruitMoney < firstFee) {
            return null;
        }
        if (returnFee > 0 && firstAway.recruitMoney < returnFee) {
            return null;
        }
        ArrayList<OocContractGame> games = new ArrayList<>();
        games.add(new OocContractGame(
                startYear,
                firstHome.abbr,
                firstAway.abbr,
                firstFee,
                NilMoney.buyGameWinBonus(firstFee)));
        games.add(new OocContractGame(
                returnYear,
                firstAway.abbr,
                firstHome.abbr,
                returnFee,
                NilMoney.buyGameWinBonus(returnFee)));
        int length = returnYear - startYear + 1;
        int buyout = NilMoney.oocCancelBuyout(
                OocContract.Type.HOME_AND_HOME, firstFee + returnFee, 2, 2);
        OocContract contract = new OocContract(
                "C" + (nextId++),
                teamA.abbr,
                teamB.abbr,
                startYear,
                length,
                OocContract.Type.HOME_AND_HOME,
                returnYear,
                buyout,
                games);
        contracts.add(contract);
        return contract;
    }

    /**
     * Signs a 2-for-1 series: power hosts twice, soft hosts once.
     * Power pays {@link NilMoney#buyGameGuarantee} on each power-home date.
     *
     * @param aHomesFirst whether {@code teamA} hosts game one
     * @param softHomeOffset years after start for the soft-home (or second) leg (1–6)
     */
    public OocContract signTwoForOne(
            Team teamA, Team teamB, int startYear, int softHomeOffset, boolean aHomesFirst) {
        if (teamA == null || teamB == null || teamA == teamB) {
            return null;
        }
        if (teamA.conference.equals(teamB.conference)) {
            return null;
        }
        int gap = Math.abs(teamA.programProfile.scheduleTier - teamB.programProfile.scheduleTier);
        if (gap <= NilMoney.PEER_SERIES_TIER_GAP) {
            return null;
        }
        int offset = Math.max(1, Math.min(6, softHomeOffset));
        int midYear = startYear + offset;
        int lastYear = midYear + 1;
        if (alreadyContracted(teamA, teamB, startYear)
                || alreadyContracted(teamA, teamB, midYear)
                || alreadyContracted(teamA, teamB, lastYear)) {
            return null;
        }
        Team power = teamA.programProfile.scheduleTier >= teamB.programProfile.scheduleTier
                ? teamA : teamB;
        Team soft = power == teamA ? teamB : teamA;
        int guarantee = NilMoney.buyGameGuarantee(power.programProfile, soft.programProfile);
        int winBonus = NilMoney.buyGameWinBonus(guarantee);
        if (power.recruitMoney < guarantee * 2) {
            return null;
        }
        Team firstHome = aHomesFirst ? teamA : teamB;
        boolean powerHostsFirst = firstHome == power;
        ArrayList<OocContractGame> games = new ArrayList<>();
        if (powerHostsFirst) {
            games.add(new OocContractGame(
                    startYear, power.abbr, soft.abbr, guarantee, winBonus));
            games.add(new OocContractGame(midYear, soft.abbr, power.abbr, 0, 0));
            games.add(new OocContractGame(
                    lastYear, power.abbr, soft.abbr, guarantee, winBonus));
        } else {
            games.add(new OocContractGame(startYear, soft.abbr, power.abbr, 0, 0));
            games.add(new OocContractGame(
                    midYear, power.abbr, soft.abbr, guarantee, winBonus));
            games.add(new OocContractGame(
                    lastYear, power.abbr, soft.abbr, guarantee, winBonus));
        }
        int length = lastYear - startYear + 1;
        int buyout = NilMoney.oocCancelBuyout(
                OocContract.Type.TWO_FOR_ONE, guarantee * 2, 3, 3);
        OocContract contract = new OocContract(
                "C" + (nextId++),
                teamA.abbr,
                teamB.abbr,
                startYear,
                length,
                OocContract.Type.TWO_FOR_ONE,
                lastYear,
                buyout,
                games);
        contracts.add(contract);
        return contract;
    }

    /**
     * Cancels an unsettled deal, charging {@code cancellingTeam} the buyout.
     *
     * @return true if cancelled
     */
    public boolean cancel(String contractId, Team cancellingTeam) {
        Iterator<OocContract> it = contracts.iterator();
        while (it.hasNext()) {
            OocContract c = it.next();
            if (!c.id.equals(contractId)) {
                continue;
            }
            int year = league.getYear();
            OocContractGame thisYear = c.gameForYear(year);
            if (thisYear != null && thisYear.settled) {
                return false;
            }
            clearScheduleForContract(c);
            if (cancellingTeam != null) {
                int fee = refreshBuyout(c);
                chargeTeam(cancellingTeam, fee, /*donorHitIfShort*/ true);
            }
            it.remove();
            return true;
        }
        return false;
    }

    /** @deprecated use {@link #cancel(String, Team)} */
    @Deprecated
    public boolean cancel(String contractId) {
        return cancel(contractId, null);
    }

    /**
     * Removes deals past their fulfill-by year that still have unsettled games.
     * Charges breach fines to both parties (split) when possible.
     *
     * @return number of contracts breached
     */
    public int enforceBreaches() {
        int year = league.getYear();
        int breached = 0;
        Iterator<OocContract> it = contracts.iterator();
        while (it.hasNext()) {
            OocContract c = it.next();
            if (!c.hasUnsettledGames()) {
                continue;
            }
            if (c.mustFulfillByYear >= year) {
                continue;
            }
            int fine = NilMoney.oocBreachFine(refreshBuyout(c));
            Team a = league.findTeamAbbr(c.teamA);
            Team b = league.findTeamAbbr(c.teamB);
            int half = fine / 2;
            if (a != null) {
                chargeTeam(a, half, true);
            }
            if (b != null) {
                chargeTeam(b, fine - half, true);
            }
            clearScheduleForContract(c);
            recentBreachNotices.add(
                    c.teamA + "–" + c.teamB + " OOC deal breached (past " + c.mustFulfillByYear
                            + "); fine " + NilMoney.format(fine));
            it.remove();
            breached++;
        }
        return breached;
    }

    private int refreshBuyout(OocContract c) {
        int year = league.getYear();
        int remaining = c.remainingGuaranteeTotal(year);
        return Math.max(
                c.buyout,
                NilMoney.oocCancelBuyout(
                        c.type, remaining, c.lengthYears, c.unsettledGameCount(year)));
    }

    private void chargeTeam(Team team, int amount, boolean donorHitIfShort) {
        if (team == null || amount <= 0) {
            return;
        }
        int paid = Math.min(team.recruitMoney, amount);
        team.recruitMoney -= paid;
        if (team.recruitMoney < 0) {
            team.recruitMoney = 0;
        }
        if (donorHitIfShort && paid < amount) {
            int oldPower = team.programProfile.programPower;
            int oldDonors = team.programProfile.donors;
            team.programProfile.donors = Math.max(25, team.programProfile.donors - 1);
            team.programProfile.refreshDerived(Conference.mediaShareFor(team.conference));
            team.programProfile.diffDonors += team.programProfile.donors - oldDonors;
            team.programProfile.diffProgramPower += team.programProfile.programPower - oldPower;
        }
    }

    private void clearScheduleForContract(OocContract c) {
        int year = league.getYear();
        OocContractGame cg = c.gameForYear(year);
        if (cg == null || cg.settled) {
            return;
        }
        Team home = league.findTeamAbbr(cg.homeAbbr);
        Team away = league.findTeamAbbr(cg.awayAbbr);
        if (home == null) {
            return;
        }
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Game game = home.gameSchedule.get(week);
            if (game == null) {
                continue;
            }
            if (c.id.equals(game.contractId)
                    || (game.homeTeam == home
                    && game.awayTeam == away
                    && isOocGame(game))) {
                OocScheduleBuilder.clearUserOocGame(home, week);
                break;
            }
        }
    }

    private static boolean isOocGame(Game game) {
        return game != null && (
                "OOC".equals(game.gameName)
                        || "OOC Rivalry".equals(game.gameName)
                        || "Rivalry Game OOC".equals(game.gameName));
    }

    /**
     * Places this year's contracted games into open OOC weeks.
     *
     * @return number of games placed
     */
    public int materializeCurrentYear() {
        int year = league.getYear();
        int placed = 0;
        for (OocContract contract : contracts) {
            OocContractGame cg = contract.gameForYear(year);
            if (cg == null || cg.settled) {
                continue;
            }
            if (placeContractGame(contract, cg)) {
                placed++;
            }
        }
        return placed;
    }

    /**
     * True when an unsettled game may still change week/year (before fulfill-by).
     */
    public boolean canReschedule(String contractId, int fromYear) {
        OocContract contract = findById(contractId);
        if (contract == null) {
            return false;
        }
        int leagueYear = league.getYear();
        if (leagueYear > contract.mustFulfillByYear) {
            return false;
        }
        OocContractGame game = contract.gameForYear(fromYear);
        return game != null && !game.settled && fromYear >= leagueYear;
    }

    /** Years this game may move to (inclusive), including its current year. */
    public List<Integer> eligibleRescheduleYears(String contractId, int fromYear) {
        ArrayList<Integer> out = new ArrayList<>();
        if (!canReschedule(contractId, fromYear)) {
            return out;
        }
        OocContract contract = findById(contractId);
        OocContractGame game = contract.gameForYear(fromYear);
        Team home = league.findTeamAbbr(game.homeAbbr);
        Team away = league.findTeamAbbr(game.awayAbbr);
        int leagueYear = league.getYear();
        for (int y = leagueYear; y <= contract.mustFulfillByYear; y++) {
            if (y != fromYear) {
                if (contract.gameForYear(y) != null) {
                    continue;
                }
                if (home != null && away != null) {
                    OocContract other = findContractBetween(home.abbr, away.abbr, y);
                    if (other != null && !other.id.equals(contract.id)) {
                        continue;
                    }
                }
            }
            out.add(y);
        }
        return out;
    }

    /**
     * Weeks usable as preferred/current placement when moving a game from {@code fromYear}
     * to {@code targetYear}.
     * Current league year: shared open weeks (plus the week already holding this deal).
     * Future years: all regular-season weeks as preferred tips (validated at materialize).
     */
    public List<Integer> eligibleRescheduleWeeks(
            String contractId, int fromYear, int targetYear) {
        ArrayList<Integer> out = new ArrayList<>();
        if (!canReschedule(contractId, fromYear)) {
            return out;
        }
        OocContract contract = findById(contractId);
        OocContractGame cg = contract.gameForYear(fromYear);
        if (cg == null) {
            return out;
        }
        Team home = league.findTeamAbbr(cg.homeAbbr);
        Team away = league.findTeamAbbr(cg.awayAbbr);
        if (home == null || away == null) {
            return out;
        }
        int leagueYear = league.getYear();
        if (targetYear != leagueYear) {
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                out.add(week);
            }
            return out;
        }
        int currentWeek = findPlacedWeek(contract);
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (week == currentWeek) {
                out.add(week);
                continue;
            }
            if (home.isOpenOocWeek(week) && away.isOpenOocWeek(week)) {
                out.add(week);
            }
        }
        return out;
    }

    /**
     * Moves an unsettled contract game to another year within the fulfill-by window.
     * Home/away and money terms stay fixed.
     */
    public boolean rescheduleYear(String contractId, int fromYear, int toYear) {
        if (!canReschedule(contractId, fromYear)) {
            return false;
        }
        OocContract contract = findById(contractId);
        if (toYear < league.getYear() || toYear > contract.mustFulfillByYear) {
            return false;
        }
        if (fromYear == toYear) {
            return true;
        }
        if (contract.gameForYear(toYear) != null) {
            return false;
        }
        OocContractGame old = contract.gameForYear(fromYear);
        Team home = league.findTeamAbbr(old.homeAbbr);
        Team away = league.findTeamAbbr(old.awayAbbr);
        if (home == null || away == null) {
            return false;
        }
        OocContract other = findContractBetween(home.abbr, away.abbr, toYear);
        if (other != null && !other.id.equals(contract.id)) {
            return false;
        }
        if (fromYear == league.getYear()) {
            clearScheduleForContract(contract);
        }
        OocContractGame moved = old.withYear(toYear);
        if (!replaceGame(contract, old, moved)) {
            return false;
        }
        if (toYear == league.getYear()) {
            return placeContractGame(contract, moved);
        }
        return true;
    }

    /**
     * Sets preferred week; for the current year, moves the placed game when possible.
     */
    public boolean rescheduleWeek(String contractId, int year, int newWeek) {
        if (!canReschedule(contractId, year)) {
            return false;
        }
        if (newWeek < 0 || newWeek >= League.REGULAR_SEASON_WEEKS) {
            return false;
        }
        OocContract contract = findById(contractId);
        OocContractGame cg = contract.gameForYear(year);
        Team home = league.findTeamAbbr(cg.homeAbbr);
        Team away = league.findTeamAbbr(cg.awayAbbr);
        if (home == null || away == null) {
            return false;
        }
        cg.preferredWeek = newWeek;
        if (year != league.getYear()) {
            return true;
        }
        int currentWeek = findPlacedWeek(contract);
        if (currentWeek == newWeek) {
            return true;
        }
        if (currentWeek >= 0) {
            clearScheduleForContract(contract);
        } else if (alreadyOnSchedule(home, away)) {
            return false;
        }
        if (!home.isOpenOocWeek(newWeek) || !away.isOpenOocWeek(newWeek)) {
            // Keep preferred tip; try any shared open week so the deal stays on the slate.
            int fallback = findSharedOpenWeek(home, away);
            if (fallback < 0) {
                return false;
            }
            return OocScheduleBuilder.placeFixedHomeOocGame(home, away, fallback, contract.id);
        }
        return OocScheduleBuilder.placeFixedHomeOocGame(home, away, newWeek, contract.id);
    }

    private boolean placeContractGame(OocContract contract, OocContractGame cg) {
        Team home = league.findTeamAbbr(cg.homeAbbr);
        Team away = league.findTeamAbbr(cg.awayAbbr);
        if (home == null || away == null) {
            return false;
        }
        if (alreadyOnSchedule(home, away)) {
            return false;
        }
        int week = -1;
        if (cg.preferredWeek >= 0
                && home.isOpenOocWeek(cg.preferredWeek)
                && away.isOpenOocWeek(cg.preferredWeek)) {
            week = cg.preferredWeek;
        } else {
            week = findSharedOpenWeek(home, away);
        }
        if (week < 0) {
            return false;
        }
        return OocScheduleBuilder.placeFixedHomeOocGame(home, away, week, contract.id);
    }

    private int findPlacedWeek(OocContract contract) {
        int year = league.getYear();
        OocContractGame cg = contract.gameForYear(year);
        if (cg == null) {
            return -1;
        }
        Team home = league.findTeamAbbr(cg.homeAbbr);
        if (home == null) {
            return -1;
        }
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            Game game = home.gameSchedule.get(week);
            if (game != null && contract.id.equals(game.contractId)) {
                return week;
            }
        }
        return -1;
    }

    private static boolean replaceGame(
            OocContract contract, OocContractGame oldGame, OocContractGame newGame) {
        for (int i = 0; i < contract.games.size(); i++) {
            if (contract.games.get(i) == oldGame
                    || (contract.games.get(i).year == oldGame.year
                    && contract.games.get(i).homeAbbr.equals(oldGame.homeAbbr)
                    && contract.games.get(i).awayAbbr.equals(oldGame.awayAbbr))) {
                contract.games.set(i, newGame);
                return true;
            }
        }
        return false;
    }

    public void settlePlayedGame(Game game) {
        if (game == null || game.homeTeam == null || game.awayTeam == null) {
            return;
        }
        String contractId = game.contractId;
        OocContract contract = null;
        if (contractId != null) {
            contract = findById(contractId);
        }
        if (contract == null) {
            contract = findContractBetween(
                    game.homeTeam.abbr, game.awayTeam.abbr, league.getYear());
        }
        if (contract == null) {
            return;
        }
        OocContractGame cg = contract.gameForYear(league.getYear());
        if (cg == null || cg.settled) {
            return;
        }
        if (!cg.homeAbbr.equals(game.homeTeam.abbr) || !cg.awayAbbr.equals(game.awayTeam.abbr)) {
            return;
        }
        transfer(game.homeTeam, game.awayTeam, cg.guarantee);
        if (game.awayScore > game.homeScore && cg.winBonus > 0) {
            transfer(game.homeTeam, game.awayTeam, cg.winBonus);
        }
        cg.settled = true;
        pruneFinished();
    }

    private static void transfer(Team from, Team to, int amount) {
        if (amount <= 0) {
            return;
        }
        int paid = Math.min(from.recruitMoney, amount);
        from.recruitMoney -= paid;
        to.recruitMoney += paid;
        if (from.recruitMoney < 0) {
            from.recruitMoney = 0;
        }
    }

    public void pruneFinished() {
        Iterator<OocContract> it = contracts.iterator();
        while (it.hasNext()) {
            OocContract c = it.next();
            if (c.isFullySettled() || !c.hasFutureGames(league.getYear())) {
                boolean anyFuture = false;
                for (OocContractGame g : c.games) {
                    if (g.year > league.getYear() || (g.year == league.getYear() && !g.settled)) {
                        anyFuture = true;
                        break;
                    }
                }
                if (!anyFuture && c.isFullySettled()) {
                    it.remove();
                } else if (!c.hasFutureGames(league.getYear()) && allPastSettledOrGone(c)) {
                    it.remove();
                }
            }
        }
    }

    private boolean allPastSettledOrGone(OocContract c) {
        for (OocContractGame g : c.games) {
            if (g.year >= league.getYear() && !g.settled) {
                return false;
            }
        }
        for (OocContractGame g : c.games) {
            if (g.year > league.getYear()) {
                return false;
            }
        }
        return true;
    }

    /** Tier gap at which a program is clearly buying a lesser opponent. */
    private static final int BUY_GAME_TIER_GAP = 12;
    /** Tier distance from league average that marks a power / soft program. */
    private static final int BAND_TIER_MARGIN = 8;
    /** Ceiling on CPU deals per pass so scheduling stays fast. */
    private static final int AUTO_DEAL_LIMIT = 45;
    /** How often a program may be booked as someone's buy-game visitor per pass. */
    private static final int MAX_SOFT_BOOKINGS = 2;
    /** Gaps the market uses between series legs, matching real deferred H&H deals. */
    private static final int[] SERIES_LEG_OFFSETS = {2, 3, 4};

    /**
     * AI: fill CPU out-of-conference slates with a realistic market mix — home buy
     * games for the big programs, peer home-and-homes, and the occasional 2-for-1
     * when a power wants a date at a smaller school.
     *
     * <p>Deferred until {@link League#userTeam} is set so a yet-to-be-picked team is
     * not pre-loaded. Never signs deals involving the user / user-controlled team.</p>
     */
    public void autoSignFutureDeals(List<Team> teams) {
        if (league.userTeam == null || teams == null) {
            return;
        }
        int year = league.getYear();
        List<Team> sorted = byScheduleTierDesc(teams);
        int averageTier = averageScheduleTier(sorted);
        ArrayList<String> softBookings = new ArrayList<>();
        int signed = 0;

        for (Team team : sorted) {
            if (signed >= AUTO_DEAL_LIMIT) {
                break;
            }
            if (isUserSide(team) || countOpenOoc(team) < 1) {
                continue;
            }
            int tier = team.programProfile.scheduleTier;
            boolean isPower = tier >= averageTier + BAND_TIER_MARGIN;
            boolean isSoft = tier <= averageTier - BAND_TIER_MARGIN;

            if (isPower) {
                // A power's road date at a smaller school comes as a 2-for-1, never
                // as a single the smaller school has to bankroll.
                if (prefersTwoForOne(team)
                        && signTwoForOneWith(team, sorted, year + 1, softBookings, null)) {
                    signed++;
                }
                for (int offset = 1; offset <= 2 && signed < AUTO_DEAL_LIMIT; offset++) {
                    if (signBuyGameWith(team, sorted, year + offset, softBookings, null)) {
                        signed++;
                    }
                }
                if (signPeerSeriesWith(team, sorted, year + 2, null)) {
                    signed++;
                }
            } else if (isSoft) {
                if (signPeerSeriesWith(team, sorted, year + 1, null)) {
                    signed++;
                }
            } else {
                if (signBuyGameWith(team, sorted, year + 1, softBookings, null)) {
                    signed++;
                }
                if (signPeerSeriesWith(team, sorted, year + 2, null)) {
                    signed++;
                }
            }
        }
    }

    /**
     * Proposes a small future-contract slate for the user using the same market mix
     * the CPU teams follow. Replaces any prior suggestion batch first.
     *
     * @return number of contracts signed
     */
    public int suggestUserFutureDeals(Team user, List<Team> teams) {
        if (user == null || teams == null) {
            return 0;
        }
        revertSuggestedUserDeals();
        int year = league.getYear();
        List<Team> sorted = byScheduleTierDesc(teams);
        int averageTier = averageScheduleTier(sorted);
        ArrayList<String> softBookings = new ArrayList<>();
        int tier = user.programProfile.scheduleTier;
        int signed = 0;
        final int maxDeals = 3;

        if (tier >= averageTier + BAND_TIER_MARGIN) {
            if (signBuyGameWith(user, sorted, year + 1, softBookings, suggestedDealIds)) {
                signed++;
            }
            if (signed < maxDeals
                    && signTwoForOneWith(user, sorted, year + 2, softBookings, suggestedDealIds)) {
                signed++;
            }
        } else if (tier <= averageTier - BAND_TIER_MARGIN) {
            // Soft programs get paid to travel rather than buying a home marquee.
            if (signRoadPaydayFor(user, sorted, year + 1, suggestedDealIds)) {
                signed++;
            }
        } else if (signBuyGameWith(user, sorted, year + 1, softBookings, suggestedDealIds)) {
            signed++;
        }

        for (int offset = 1; offset <= 2 && signed < maxDeals; offset++) {
            if (signPeerSeriesWith(user, sorted, year + offset, suggestedDealIds)) {
                signed++;
            }
        }
        return signed;
    }

    /** Books {@code power} a home buy game against the best available smaller program. */
    private boolean signBuyGameWith(
            Team power,
            List<Team> candidates,
            int year,
            List<String> softBookings,
            List<String> trackIds) {
        for (int i = candidates.size() - 1; i >= 0; i--) {
            Team soft = candidates.get(i);
            if (!isTradePartner(power, soft, year)) {
                continue;
            }
            if (power.programProfile.scheduleTier - soft.programProfile.scheduleTier
                    < BUY_GAME_TIER_GAP) {
                continue;
            }
            if (Collections.frequency(softBookings, soft.abbr) >= MAX_SOFT_BOOKINGS) {
                continue;
            }
            if (power.recruitMoney < NilMoney.buyGameGuarantee(
                    power.programProfile, soft.programProfile)) {
                continue;
            }
            OocContract contract = signBuyGame(power, soft, year, 1);
            if (contract != null) {
                softBookings.add(soft.abbr);
                if (trackIds != null) {
                    trackIds.add(contract.id);
                }
                return true;
            }
        }
        return false;
    }

    /** Books {@code soft} a road payday at the biggest program that can afford it. */
    private boolean signRoadPaydayFor(
            Team soft, List<Team> candidates, int year, List<String> trackIds) {
        for (Team power : candidates) {
            if (!isTradePartner(soft, power, year)) {
                continue;
            }
            if (power.programProfile.scheduleTier - soft.programProfile.scheduleTier
                    < BUY_GAME_TIER_GAP) {
                continue;
            }
            if (power.recruitMoney < NilMoney.buyGameGuarantee(
                    power.programProfile, soft.programProfile)) {
                continue;
            }
            OocContract contract = signBuyGame(power, soft, year, 1);
            if (contract != null) {
                if (trackIds != null) {
                    trackIds.add(contract.id);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Books a free peer home-and-home, preferring a rivalry when one exists. The return
     * leg lands two to four years out rather than the following season.
     */
    private boolean signPeerSeriesWith(
            Team team, List<Team> candidates, int startYear, List<String> trackIds) {
        Team best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Team other : candidates) {
            if (!isTradePartner(team, other, startYear)) {
                continue;
            }
            if (openSeriesReturnYear(team, other, startYear) == 0) {
                continue;
            }
            int gap = Math.abs(
                    team.programProfile.scheduleTier - other.programProfile.scheduleTier);
            if (gap > NilMoney.PEER_SERIES_TIER_GAP) {
                continue;
            }
            int score = Team.strongestRivalryBetween(team, other) * 10 - gap;
            if (score > bestScore) {
                bestScore = score;
                best = other;
            }
        }
        if (best == null) {
            return false;
        }
        int returnYear = openSeriesReturnYear(team, best, startYear);
        if (returnYear == 0) {
            return false;
        }
        OocContract contract = signHomeAndHome(team, best, startYear, returnYear, true);
        if (contract == null) {
            return false;
        }
        if (trackIds != null) {
            trackIds.add(contract.id);
        }
        return true;
    }

    /** Books a 2-for-1 so a power can play at a smaller school without billing it. */
    private boolean signTwoForOneWith(
            Team power,
            List<Team> candidates,
            int startYear,
            List<String> softBookings,
            List<String> trackIds) {
        int guaranteeBudget = power.recruitMoney;
        for (int i = candidates.size() - 1; i >= 0; i--) {
            Team soft = candidates.get(i);
            if (!isTradePartner(power, soft, startYear)) {
                continue;
            }
            int gap = power.programProfile.scheduleTier - soft.programProfile.scheduleTier;
            if (gap <= NilMoney.PEER_SERIES_TIER_GAP) {
                continue;
            }
            if (Collections.frequency(softBookings, soft.abbr) >= MAX_SOFT_BOOKINGS) {
                continue;
            }
            if (guaranteeBudget < NilMoney.buyGameGuarantee(
                    power.programProfile, soft.programProfile) * 2) {
                continue;
            }
            OocContract contract = null;
            for (int offset : preferredSeriesOffsets(power, soft)) {
                contract = signTwoForOne(power, soft, startYear, offset, true);
                if (contract != null) {
                    break;
                }
            }
            if (contract != null) {
                softBookings.add(soft.abbr);
                if (trackIds != null) {
                    trackIds.add(contract.id);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isTradePartner(Team team, Team other, int year) {
        if (other == null || other == team || isUserSide(other)) {
            return false;
        }
        if (team.conference.equals(other.conference)) {
            return false;
        }
        return !alreadyContracted(team, other, year);
    }

    /** Deterministic stand-in for "some powers prefer a 2-for-1 this cycle". */
    private static boolean prefersTwoForOne(Team power) {
        return Math.abs(power.abbr.hashCode()) % 5 == 0;
    }

    /**
     * Per-pair ordering of {@link #SERIES_LEG_OFFSETS}: a matchup always favours the
     * same gap between legs, falling back to the others when a year is already booked.
     */
    private static int[] preferredSeriesOffsets(Team a, Team b) {
        String key = a.abbr.compareTo(b.abbr) <= 0 ? a.abbr + b.abbr : b.abbr + a.abbr;
        int rotation = Math.abs(key.hashCode()) % SERIES_LEG_OFFSETS.length;
        int[] ordered = new int[SERIES_LEG_OFFSETS.length];
        for (int i = 0; i < ordered.length; i++) {
            ordered[i] = SERIES_LEG_OFFSETS[(rotation + i) % SERIES_LEG_OFFSETS.length];
        }
        return ordered;
    }

    /** Preferred return year still open for this pair, or 0 when every gap is taken. */
    private int openSeriesReturnYear(Team a, Team b, int startYear) {
        for (int offset : preferredSeriesOffsets(a, b)) {
            if (!alreadyContracted(a, b, startYear + offset)) {
                return startYear + offset;
            }
        }
        return 0;
    }

    private static List<Team> byScheduleTierDesc(List<Team> teams) {
        ArrayList<Team> sorted = new ArrayList<>(teams);
        Collections.sort(sorted, (a, b) -> {
            int cmp = Integer.compare(
                    b.programProfile.scheduleTier, a.programProfile.scheduleTier);
            return cmp != 0 ? cmp : a.abbr.compareTo(b.abbr);
        });
        return sorted;
    }

    private static int averageScheduleTier(List<Team> teams) {
        if (teams.isEmpty()) {
            return 50;
        }
        int sum = 0;
        for (Team t : teams) {
            sum += t.programProfile.scheduleTier;
        }
        return sum / teams.size();
    }

    /**
     * Voids the current suggestion batch with no buyout.
     *
     * @return number of contracts removed
     */
    public int revertSuggestedUserDeals() {
        int removed = 0;
        for (String id : new ArrayList<>(suggestedDealIds)) {
            if (cancel(id, null)) {
                removed++;
            }
        }
        suggestedDealIds.clear();
        return removed;
    }

    public boolean hasSuggestedUserDeals() {
        if (suggestedDealIds.isEmpty()) {
            return false;
        }
        for (String id : suggestedDealIds) {
            if (findById(id) != null) {
                return true;
            }
        }
        suggestedDealIds.clear();
        return false;
    }

    private boolean isUserSide(Team team) {
        if (team == null) {
            return false;
        }
        if (team.userControlled) {
            return true;
        }
        return league.userTeam != null && league.userTeam == team;
    }

    private static int countOpenOoc(Team team) {
        int n = 0;
        for (int w = 0; w < League.REGULAR_SEASON_WEEKS; w++) {
            if (team.isOpenOocWeek(w)) {
                n++;
            }
        }
        return n;
    }

    private static boolean alreadyOnSchedule(Team a, Team b) {
        for (Game g : a.gameSchedule) {
            if (g == null) {
                continue;
            }
            Team opp = g.homeTeam == a ? g.awayTeam : g.homeTeam;
            if (opp == b) {
                return true;
            }
        }
        return false;
    }

    private static int findSharedOpenWeek(Team a, Team b) {
        for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
            if (a.isOpenOocWeek(week) && b.isOpenOocWeek(week)) {
                return week;
            }
        }
        return -1;
    }

    /**
     * After schedule restore, attach {@link Game#contractId} for this year's contracted matchups.
     */
    public void relinkScheduleContractIds() {
        int year = league.getYear();
        for (OocContract contract : contracts) {
            OocContractGame cg = contract.gameForYear(year);
            if (cg == null || cg.settled) {
                continue;
            }
            Team home = league.findTeamAbbr(cg.homeAbbr);
            Team away = league.findTeamAbbr(cg.awayAbbr);
            if (home == null || away == null) {
                continue;
            }
            for (int week = 0; week < League.REGULAR_SEASON_WEEKS; week++) {
                Game game = home.gameSchedule.get(week);
                if (game == null) {
                    continue;
                }
                if (game.homeTeam == home && game.awayTeam == away) {
                    game.contractId = contract.id;
                    break;
                }
            }
        }
    }

    public void retargetAbbr(String oldAbbr, String newAbbr) {
        if (oldAbbr == null || newAbbr == null || oldAbbr.equals(newAbbr)) {
            return;
        }
        ArrayList<OocContract> updated = new ArrayList<>();
        for (OocContract c : contracts) {
            String a = c.teamA.equals(oldAbbr) ? newAbbr : c.teamA;
            String b = c.teamB.equals(oldAbbr) ? newAbbr : c.teamB;
            ArrayList<OocContractGame> games = new ArrayList<>();
            for (OocContractGame g : c.games) {
                String home = g.homeAbbr.equals(oldAbbr) ? newAbbr : g.homeAbbr;
                String away = g.awayAbbr.equals(oldAbbr) ? newAbbr : g.awayAbbr;
                OocContractGame ng = new OocContractGame(g.year, home, away, g.guarantee, g.winBonus);
                ng.settled = g.settled;
                games.add(ng);
            }
            updated.add(new OocContract(
                    c.id, a, b, c.startYear, c.lengthYears, c.type,
                    c.mustFulfillByYear, c.buyout, games));
        }
        contracts.clear();
        contracts.addAll(updated);
    }

    public void appendSave(StringBuilder sb) {
        sb.append("OOC_CONTRACTS\n");
        sb.append("NEXT_ID,").append(nextId).append("\n");
        for (OocContract c : contracts) {
            sb.append(c.encode()).append("\n");
        }
        sb.append("END_OOC_CONTRACTS\n");
    }

    public void restore(BufferedReader reader) throws IOException {
        contracts.clear();
        String line = reader.readLine();
        if (line != null && line.startsWith("NEXT_ID,")) {
            nextId = Integer.parseInt(line.substring("NEXT_ID,".length()).trim());
            line = reader.readLine();
        }
        while (line != null && !line.equals("END_OOC_CONTRACTS")) {
            if (!line.isEmpty()) {
                OocContract c = OocContract.parse(line);
                contracts.add(c);
                try {
                    int idNum = Integer.parseInt(c.id.replace("C", ""));
                    if (idNum >= nextId) {
                        nextId = idNum + 1;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            line = reader.readLine();
        }
    }

    /** Replace the ledger from typed save data. */
    public void replaceAll(int nextIdValue, List<OocContract> restored) {
        contracts.clear();
        if (restored != null) {
            contracts.addAll(restored);
        }
        nextId = Math.max(1, nextIdValue);
        for (OocContract c : contracts) {
            try {
                int idNum = Integer.parseInt(c.id.replace("C", ""));
                if (idNum >= nextId) {
                    nextId = idNum + 1;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
