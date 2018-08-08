package it.stream.streamit;

public class YearList {
    private String year;
    private String imageUrl;

    public String getYear() {
        return year;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    YearList(String year, String imageUrl) {

        this.year = year;
        this.imageUrl = imageUrl;
    }
}
