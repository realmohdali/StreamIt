package it.stream.streamit.dataList;

public class ArtistList {
    String artist, imageUrl;

    public ArtistList(String artist, String imageUrl) {
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
