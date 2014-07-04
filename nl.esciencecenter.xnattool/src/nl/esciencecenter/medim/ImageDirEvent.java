package nl.esciencecenter.medim;

/**
 * Event object for the ImageDirScanner to fire. <br>
 * Can be monitored during the Image Dir Scanning process.
 */
public class ImageDirEvent
{
    public static ImageDirEvent newMessageEvent(String message)
    {
        return new ImageDirEvent(null, ImageDirEventType.MESSAGE, message);
    }

    // ========
    // Instance
    // ========

    public ImageDirScanner source;

    public ImageDirEventType eventType;

    public String identifierOrMessage;

    /**
     * Optional argument array, might contain event specific objects.
     */
    public Object args[];

    public ImageDirEvent(ImageDirScanner source, ImageDirEventType eventType, String identifierOrMessage, Object... args)
    {
        this.source = source;
        this.eventType = eventType;
        this.identifierOrMessage = identifierOrMessage;
        this.args = args;
    }

    public String toString()
    {
        return "{DicomDirEvent:eventType=" + eventType + ",identifier=" + identifierOrMessage + "}";
    }

}