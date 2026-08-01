package CFBsimPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold all the all-time league season records like passing/rushing yards, TDs, etc
 * Created by Achi Jones on 3/19/2016.
 */
public class LeagueRecords {

    public class Record {
        private int number;
        private String holder;
        private int year;

        public Record(int n, String h, int y) {
            number = n;
            holder = h;
            year = y;
        }

        public int getNumber() {
            return number;
        }

        public String getHolder() {
            return holder;
        }

        public int getYear() {
            return year;
        }

        private void changeAbbr(String newAbbr) {
            String[] split = holder.split(" ");
            holder = newAbbr;
            for (int i = 1; i < split.length; ++i) {
                holder += " " + split[i];
            }
        }
    }

    private HashMap<String, Record> records;

    public final String[] recordsList = {"TEAM","Team PPG","Team Opp PPG","Team YPG","Team Opp YPG","Team TO Diff",
            "SEASON","Pass Yards","Pass TDs","Interceptions","Comp Percent",
            "Rush Yards","Rush TDs","Rush Fumbles",
            "Rec Yards","Rec TDs","Catch Percent",
            "CAREER","Career Pass Yards","Career Pass TDs","Career Interceptions",
            "Career Rush Yards","Career Rush TDs","Career Rush Fumbles",
            "Career Rec Yards","Career Rec TDs"};

    public LeagueRecords(ArrayList<String> recordStrings) {
        records = new HashMap<String, Record>();
        String[] csv;
        for (String str : recordStrings) {
            csv = str.split(",");
            records.put(csv[0], new Record(Integer.parseInt(csv[1]), csv[2], Integer.parseInt(csv[3])));
        }
    }

    public LeagueRecords() {
        records = new HashMap<String, Record>();
        records.put("TEAM", null);
        records.put("Team PPG", new Record(0, "XXX", 0));
        records.put("Team Opp PPG", new Record(1000, "XXX", 0));
        records.put("Team YPG", new Record(0, "XXX", 0));
        records.put("Team Opp YPG", new Record(1000, "XXX", 0));
        records.put("Team TO Diff", new Record(0, "XXX", 0));
        records.put("SEASON", null);
        records.put("Pass Yards", new Record(0, "XXX", 0));
        records.put("Pass TDs", new Record(0, "XXX", 0));
        records.put("Interceptions", new Record(0, "XXX", 0));
        records.put("Comp Percent", new Record(0, "XXX", 0));
        records.put("Rush Yards", new Record(0, "XXX", 0));
        records.put("Rush TDs", new Record(0, "XXX", 0));
        records.put("Rush Fumbles", new Record(0, "XXX", 0));
        records.put("Rec Yards", new Record(0, "XXX", 0));
        records.put("Rec TDs", new Record(0, "XXX", 0));
        records.put("Catch Percent", new Record(0, "XXX", 0));
        records.put("CAREER", null);
        records.put("Career Pass Yards", new Record(0, "XXX", 0));
        records.put("Career Pass TDs", new Record(0, "XXX", 0));
        records.put("Career Interceptions", new Record(0, "XXX", 0));
        records.put("Career Rush Yards", new Record(0, "XXX", 0));
        records.put("Career Rush TDs", new Record(0, "XXX", 0));
        records.put("Career Rush Fumbles", new Record(0, "XXX", 0));
        records.put("Career Rec Yards", new Record(0, "XXX", 0));
        records.put("Career Rec TDs", new Record(0, "XXX", 0));
    }

    public void checkRecord(String record, int number, String holder, int year) {
        if (record.equals("Team Opp PPG") || record.equals("Team Opp YPG")) {
            // Is a record where lower = better
            if ((records.containsKey(record) && number < records.get(record).getNumber())) {
                records.remove(record);
                records.put(record, new Record(number, holder, year));
            } else if (!records.containsKey(record)) {
                records.put(record, new Record(number, holder, year));
            }
        } else {
            // Is a record where higher = better
            if ((records.containsKey(record) && number > records.get(record).getNumber())) {
                records.remove(record);
                records.put(record, new Record(number, holder, year));
            } else if (!records.containsKey(record)) {
                records.put(record, new Record(number, holder, year));
            }
        }
    }

    public void changeAbbrRecords(String oldAbbr, String newAbbr) {
        Record r;
        for (String s : recordsList) {
            r = records.get(s);
            if (r != null && r.getHolder().split(" ")[0].equals(oldAbbr)) {
                r.changeAbbr(newAbbr);
            }
        }
    }

    public static class RecordEntry {
        public final String key;
        public final int number;
        public final String holder;
        public final int year;

        public RecordEntry(String key, int number, String holder, int year) {
            this.key = key;
            this.number = number;
            this.holder = holder;
            this.year = year;
        }
    }

    public ArrayList<RecordEntry> getOrderedRecordEntries() {
        ArrayList<RecordEntry> entries = new ArrayList<>();
        for (String s : recordsList) {
            entries.add(recordEntry(s));
        }
        return entries;
    }

    private RecordEntry recordEntry(String key) {
        if (records.containsKey(key)) {
            Record r = records.get(key);
            if (r == null) {
                return new RecordEntry(key, -1, "-1", -1);
            }
            return new RecordEntry(key, r.getNumber(), r.getHolder(), r.getYear());
        }
        return new RecordEntry("ERROR", -1, "ERROR", -1);
    }

    public String getRecordsStr() {
        StringBuilder sb = new StringBuilder();
        for (RecordEntry entry : getOrderedRecordEntries()) {
            sb.append(recordEntryCsv(entry)).append("\n");
        }
        return sb.toString();
    }

    private String recordEntryCsv(RecordEntry entry) {
        if (entry.number == -1) {
            return entry.key + ",-1,-1,-1";
        }
        return entry.key + "," + entry.number + "," + entry.holder + "," + entry.year;
    }

    /**
     * Print out string of all the records broken by a team that year
     * @return string of all records broken
     */
    public String brokenRecordsStr(int year, String abbr) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Record> r : records.entrySet()) {
            if (r.getValue() != null &&
                    r.getValue().getHolder().split(" ")[0].equals(abbr) &&
                    r.getValue().getYear() == year &&
                    !r.getKey().split(" ")[0].equals("Career")) {
                sb.append(r.getValue().getHolder() + " broke the record for " +
                    r.getKey() + " with " + r.getValue().getNumber() + "!\n");
            }
        }

        return sb.toString();
    }

}
