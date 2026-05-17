package job.model.script;

public class JobScript {
    private ScriptLanguage language;
    private String content;

    public JobScript() {
        this(ScriptLanguage.JAVA, "");
    }

    public JobScript(ScriptLanguage language, String content) {
        this.language = language;
        this.content = content;
    }

    public ScriptLanguage getLanguage() {
        return language;
    }

    public void setLanguage(ScriptLanguage language) {
        this.language = language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
