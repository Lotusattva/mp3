package fsft.wikipedia;

import fsft.fsftbuffer.Bufferable;

public class WikiPage implements Bufferable {

    private final String pageTitle;
    private final String page;

    public WikiPage(String pageTitle, String page) {
        this.pageTitle = pageTitle;
        this.page = page;
    }
    
    public String id() {
        return pageTitle;
    }

    public String getPage() {
        return page;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WikiPage)) {
            return false;
        }
        WikiPage otherPage = (WikiPage) obj;
        return pageTitle.equals(otherPage.pageTitle);
    }

    @Override
    public int hashCode() {
        return pageTitle.hashCode();
    }
}
