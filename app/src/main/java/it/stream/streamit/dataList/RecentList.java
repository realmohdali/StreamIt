package it.stream.streamit.dataList;

public class RecentList {
    String artist, year, img;

    public RecentList(String artist, String year, String img) {
        this.artist = artist;
        this.year = year;
        this.img = img;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public String getImg() {
        return img;
    }
}
