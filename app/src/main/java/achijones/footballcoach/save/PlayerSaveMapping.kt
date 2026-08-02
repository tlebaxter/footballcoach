package achijones.footballcoach.save

import CFBsimPack.Injury
import CFBsimPack.Player
import CFBsimPack.PlayerFactory
import CFBsimPack.PlayerRatings
import CFBsimPack.PlayerSeasonRecord
import CFBsimPack.PlayerSkillStats
import CFBsimPack.PositionGroup
import CFBsimPack.RosterStatus
import CFBsimPack.Team

internal object PlayerSaveMapping {
    fun fromPlayer(p: Player): PlayerDoc {
        val hasSeason = p.gamesPlayed > 0 || p.statsWins > 0 || p.seasonSnaps > 0
            || p.isInjured || p.injury != null || p.isEjected
            || hasAnySkill(p.seasonStats)
            || p.statsPrAtt > 0 || p.statsKrAtt > 0
        return PlayerDoc(
            position = p.position,
            name = p.name,
            year = p.year,
            isRedshirt = p.isRedshirt,
            ratings = fromRatings(p.ratings),
            ovr = p.ratOvr,
            improvement = p.ratImprovement,
            careerGamesPlayed = p.careerGamesPlayed,
            careerSnaps = p.careerSnaps,
            careerStats = fromSkills(p.careerStats),
            careerHeismans = p.careerHeismans,
            careerAllAmerican = p.careerAllAmerican,
            careerAllConference = p.careerAllConference,
            careerWins = p.careerWins,
            rosterStatus = (p.rosterStatus ?: RosterStatus.SCHOLARSHIP).name,
            nilDealAmount = p.nilDealAmount,
            contractYearsRemaining = p.contractYearsRemaining,
            contractLength = p.contractLength,
            retainedThisOffseason = p.retainedThisOffseason,
            depthLocked = p.depthLocked,
            homeGeoid = p.homeGeoid,
            homeCity = p.homeCity,
            homeState = p.homeState,
            yearsAtProgram = p.yearsAtProgram,
            season = if (hasSeason) {
                PlayerSeasonDoc(
                    gamesPlayed = p.gamesPlayed,
                    statsWins = p.statsWins,
                    seasonSnaps = p.seasonSnaps,
                    stats = fromSkills(p.seasonStats),
                    injuryDescription = if (p.isInjured && p.injury != null) p.injury.getDescription() else null,
                    injuryDuration = if (p.isInjured && p.injury != null) p.injury.getDuration() else 0,
                    isEjected = p.isEjected,
                    prAtt = p.statsPrAtt,
                    prYards = p.statsPrYards,
                    prTd = p.statsPrTd,
                    krAtt = p.statsKrAtt,
                    krYards = p.statsKrYards,
                    krTd = p.statsKrTd,
                    fairCatches = p.statsFairCatches,
                )
            } else {
                null
            },
            careerSeasons = p.careerSeasons.map { fromSeasonRecord(it) },
            careerPrAtt = p.careerPrAtt,
            careerPrYards = p.careerPrYards,
            careerPrTd = p.careerPrTd,
            careerKrAtt = p.careerKrAtt,
            careerKrYards = p.careerKrYards,
            careerKrTd = p.careerKrTd,
            careerFairCatches = p.careerFairCatches,
        )
    }

    fun toPlayer(doc: PlayerDoc, team: Team?): Player {
        val group = PositionGroup.fromToken(doc.position)
            ?: throw CorruptSaveException("Unknown position ${doc.position}")
        val bag = toRatings(doc.ratings)
        val p = PlayerFactory.fromRatings(group, doc.name, team, doc.year, bag, doc.isRedshirt)
        p.ratOvr = doc.ovr
        p.ratImprovement = doc.improvement
        p.careerGamesPlayed = doc.careerGamesPlayed
        p.careerSnaps = doc.careerSnaps
        applySkills(p.careerStats, doc.careerStats)
        p.careerHeismans = doc.careerHeismans
        p.careerAllAmerican = doc.careerAllAmerican
        p.careerAllConference = doc.careerAllConference
        p.careerWins = doc.careerWins
        p.rosterStatus = RosterStatus.fromString(doc.rosterStatus)
        p.nilDealAmount = doc.nilDealAmount
        p.contractYearsRemaining = doc.contractYearsRemaining
        p.contractLength = doc.contractLength
        p.retainedThisOffseason = doc.retainedThisOffseason
        p.depthLocked = doc.depthLocked
        p.homeGeoid = doc.homeGeoid
        p.homeCity = doc.homeCity
        p.homeState = doc.homeState
        p.yearsAtProgram = doc.yearsAtProgram
        if (p.homeGeoid == null || p.homeGeoid.isEmpty()) {
            CFBsimPack.GeoCatalog.get().applyHometown(p, java.util.Random(doc.name.hashCode().toLong()))
        }
        p.careerPrAtt = doc.careerPrAtt
        p.careerPrYards = doc.careerPrYards
        p.careerPrTd = doc.careerPrTd
        p.careerKrAtt = doc.careerKrAtt
        p.careerKrYards = doc.careerKrYards
        p.careerKrTd = doc.careerKrTd
        p.careerFairCatches = doc.careerFairCatches
        val season = doc.season
        if (season != null) {
            p.gamesPlayed = season.gamesPlayed
            p.statsWins = season.statsWins
            p.seasonSnaps = season.seasonSnaps
            applySkills(p.seasonStats, season.stats)
            p.isEjected = season.isEjected
            p.statsPrAtt = season.prAtt
            p.statsPrYards = season.prYards
            p.statsPrTd = season.prTd
            p.statsKrAtt = season.krAtt
            p.statsKrYards = season.krYards
            p.statsKrTd = season.krTd
            p.statsFairCatches = season.fairCatches
            val desc = season.injuryDescription
            if (desc != null && desc.isNotBlank() && season.injuryDuration > 0) {
                p.injury = Injury(season.injuryDuration, desc, p)
            }
        }
        p.careerSeasons = ArrayList(doc.careerSeasons.map { toSeasonRecord(it) })
        return p
    }

    fun fromSeasonRecord(r: PlayerSeasonRecord): PlayerSeasonRecordDoc {
        return PlayerSeasonRecordDoc(
            seasonYear = r.seasonYear,
            teamAbbr = r.teamAbbr,
            teamName = r.teamName,
            classYear = r.classYear,
            gamesPlayed = r.gamesPlayed,
            wins = r.wins,
            wonHeisman = r.wonHeisman,
            wonAllAmerican = r.wonAllAmerican,
            wonAllConference = r.wonAllConference,
            position = r.position,
            stats = SkillStatsDoc(
                passAtt = r.passAtt,
                passComp = r.passComp,
                passYards = r.passYards,
                passTd = r.passTd,
                passInt = r.passInt,
                sacked = r.sacked,
                rushAtt = r.rushAtt,
                rushYards = r.rushYards,
                rushTd = r.rushTd,
                fumbles = r.rushFumbles,
                targets = r.targets,
                receptions = r.receptions,
                recYards = r.recYards,
                recTd = r.recTd,
                drops = r.drops,
                recFumbles = r.recFumbles,
                xpAtt = r.xpAtt,
                xpMade = r.xpMade,
                fgAtt = r.fgAtt,
                fgMade = r.fgMade,
                puntAtt = r.puntAtt,
                puntYards = r.puntYards,
                tackles = r.tackles,
                tfl = r.tfl,
                sacksDef = r.sacksDef,
                defInt = r.defInt,
                passDef = r.passDef,
                forcedFumbles = r.forcedFumbles,
                fumbleRec = r.fumbleRec,
            ),
            rushFumbles = r.rushFumbles,
            prAtt = r.prAtt,
            prYards = r.prYards,
            prTd = r.prTd,
            krAtt = r.krAtt,
            krYards = r.krYards,
            krTd = r.krTd,
            fairCatches = r.fairCatches,
        )
    }

    fun toSeasonRecord(doc: PlayerSeasonRecordDoc): PlayerSeasonRecord {
        val r = PlayerSeasonRecord()
        r.seasonYear = doc.seasonYear
        r.teamAbbr = doc.teamAbbr
        r.teamName = doc.teamName
        r.classYear = doc.classYear
        r.gamesPlayed = doc.gamesPlayed
        r.wins = doc.wins
        r.wonHeisman = doc.wonHeisman
        r.wonAllAmerican = doc.wonAllAmerican
        r.wonAllConference = doc.wonAllConference
        r.position = doc.position
        r.passAtt = doc.stats.passAtt
        r.passComp = doc.stats.passComp
        r.passYards = doc.stats.passYards
        r.passTd = doc.stats.passTd
        r.passInt = doc.stats.passInt
        r.sacked = doc.stats.sacked
        r.rushAtt = doc.stats.rushAtt
        r.rushYards = doc.stats.rushYards
        r.rushTd = doc.stats.rushTd
        r.rushFumbles = doc.rushFumbles.takeIf { it != 0 } ?: doc.stats.fumbles
        r.targets = doc.stats.targets
        r.receptions = doc.stats.receptions
        r.recYards = doc.stats.recYards
        r.recTd = doc.stats.recTd
        r.drops = doc.stats.drops
        r.recFumbles = doc.stats.recFumbles
        r.xpAtt = doc.stats.xpAtt
        r.xpMade = doc.stats.xpMade
        r.fgAtt = doc.stats.fgAtt
        r.fgMade = doc.stats.fgMade
        r.puntAtt = doc.stats.puntAtt
        r.puntYards = doc.stats.puntYards
        r.tackles = doc.stats.tackles
        r.tfl = doc.stats.tfl
        r.sacksDef = doc.stats.sacksDef
        r.defInt = doc.stats.defInt
        r.passDef = doc.stats.passDef
        r.forcedFumbles = doc.stats.forcedFumbles
        r.fumbleRec = doc.stats.fumbleRec
        r.prAtt = doc.prAtt
        r.prYards = doc.prYards
        r.prTd = doc.prTd
        r.krAtt = doc.krAtt
        r.krYards = doc.krYards
        r.krTd = doc.krTd
        r.fairCatches = doc.fairCatches
        return r
    }

    fun fromRatings(r: PlayerRatings): RatingsDoc {
        return RatingsDoc(
            pot = r.pot,
            footIq = r.footIq,
            dur = r.dur,
            hgt = r.hgt,
            stre = r.stre,
            spd = r.spd,
            endu = r.endu,
            thv = r.thv,
            thp = r.thp,
            tha = r.tha,
            bsc = r.bsc,
            elu = r.elu,
            rtr = r.rtr,
            hnd = r.hnd,
            pbk = r.pbk,
            rbk = r.rbk,
            pcv = r.pcv,
            tck = r.tck,
            prs = r.prs,
            rns = r.rns,
            kpw = r.kpw,
            kac = r.kac,
            ppw = r.ppw,
            pac = r.pac,
        )
    }

    fun toRatings(doc: RatingsDoc): PlayerRatings {
        val r = PlayerRatings()
        r.pot = doc.pot
        r.footIq = doc.footIq
        r.dur = doc.dur
        r.hgt = doc.hgt
        r.stre = doc.stre
        r.spd = doc.spd
        r.endu = doc.endu
        r.thv = doc.thv
        r.thp = doc.thp
        r.tha = doc.tha
        r.bsc = doc.bsc
        r.elu = doc.elu
        r.rtr = doc.rtr
        r.hnd = doc.hnd
        r.pbk = doc.pbk
        r.rbk = doc.rbk
        r.pcv = doc.pcv
        r.tck = doc.tck
        r.prs = doc.prs
        r.rns = doc.rns
        r.kpw = doc.kpw
        r.kac = doc.kac
        r.ppw = doc.ppw
        r.pac = doc.pac
        return r
    }

    fun fromSkills(s: PlayerSkillStats): SkillStatsDoc {
        return SkillStatsDoc(
            passAtt = s.passAtt,
            passComp = s.passComp,
            passYards = s.passYards,
            passTd = s.passTd,
            passInt = s.passInt,
            sacked = s.sacked,
            rushAtt = s.rushAtt,
            rushYards = s.rushYards,
            rushTd = s.rushTd,
            fumbles = s.fumbles,
            targets = s.targets,
            receptions = s.receptions,
            recYards = s.recYards,
            recTd = s.recTd,
            drops = s.drops,
            recFumbles = s.recFumbles,
            xpAtt = s.xpAtt,
            xpMade = s.xpMade,
            fgAtt = s.fgAtt,
            fgMade = s.fgMade,
            puntAtt = s.puntAtt,
            puntYards = s.puntYards,
            tackles = s.tackles,
            tfl = s.tfl,
            sacksDef = s.sacksDef,
            defInt = s.defInt,
            passDef = s.passDef,
            forcedFumbles = s.forcedFumbles,
            fumbleRec = s.fumbleRec,
        )
    }

    fun applySkills(target: PlayerSkillStats, doc: SkillStatsDoc) {
        target.passAtt = doc.passAtt
        target.passComp = doc.passComp
        target.passYards = doc.passYards
        target.passTd = doc.passTd
        target.passInt = doc.passInt
        target.sacked = doc.sacked
        target.rushAtt = doc.rushAtt
        target.rushYards = doc.rushYards
        target.rushTd = doc.rushTd
        target.fumbles = doc.fumbles
        target.targets = doc.targets
        target.receptions = doc.receptions
        target.recYards = doc.recYards
        target.recTd = doc.recTd
        target.drops = doc.drops
        target.recFumbles = doc.recFumbles
        target.xpAtt = doc.xpAtt
        target.xpMade = doc.xpMade
        target.fgAtt = doc.fgAtt
        target.fgMade = doc.fgMade
        target.puntAtt = doc.puntAtt
        target.puntYards = doc.puntYards
        target.tackles = doc.tackles
        target.tfl = doc.tfl
        target.sacksDef = doc.sacksDef
        target.defInt = doc.defInt
        target.passDef = doc.passDef
        target.forcedFumbles = doc.forcedFumbles
        target.fumbleRec = doc.fumbleRec
    }

    private fun hasAnySkill(s: PlayerSkillStats): Boolean {
        return s.passAtt != 0 || s.passComp != 0 || s.passYards != 0 || s.passTd != 0
            || s.passInt != 0 || s.sacked != 0
            || s.rushAtt != 0 || s.rushYards != 0 || s.rushTd != 0 || s.fumbles != 0
            || s.targets != 0 || s.receptions != 0 || s.recYards != 0 || s.recTd != 0
            || s.drops != 0 || s.recFumbles != 0
            || s.xpAtt != 0 || s.xpMade != 0 || s.fgAtt != 0 || s.fgMade != 0
            || s.puntAtt != 0 || s.puntYards != 0
            || s.tackles != 0 || s.tfl != 0 || s.sacksDef != 0 || s.defInt != 0
            || s.passDef != 0 || s.forcedFumbles != 0 || s.fumbleRec != 0
    }
}
