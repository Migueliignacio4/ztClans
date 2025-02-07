package net.razt.ztClans.utils;

public class ClanRank {
    private String rankName;
    private String rankType;

    public ClanRank(String rankName, String rankType) {
        this.rankName = rankName;
        this.rankType = rankType;
    }

    public String getRankName() {
        return rankName;
    }

    public String getRankType() {
        return rankType;
    }

    @Override
    public String toString() {
        return "ClanRank{name='" + rankName + "', type='" + rankType + "'}";
    }
}
