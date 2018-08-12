package it.stream.streamit.dataList;

public class ArtistList {
    String artist, imageUrl, nationality, years;

    public ArtistList(String artist, String imageUrl, String nationality, String years) {
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.nationality = nationality;
        this.years = years;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getNationality() {
        return nationality;
    }

    public String getYears() {
        return years;
    }
}
