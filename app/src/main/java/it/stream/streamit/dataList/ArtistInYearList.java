package it.stream.streamit.dataList;

public class ArtistInYearList {
    private String artist;
    private String imageUrl;

    public ArtistInYearList(String artist, String imageUrl) {
        this.artist = artist;
        this.imageUrl = imageUrl;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
