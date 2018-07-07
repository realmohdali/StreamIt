package it.stream.streamit;

public class ListItem {
    private String title;
    private String artist;
    private String imageUrl;
    private String URL;
    private String year;

    public ListItem(String title, String artist, String imageUrl, String URL, String year) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.URL = URL;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getURL() {
        return URL;
    }

    public String getYear() {
        return year;
    }
}
