import com.kay.cyberterrarium.JVMPlatform;

import static com.kay.cyberterrarium.PlatformKt.getPlatform;

public class Greeting {
    private static JVMPlatform platform = getPlatform();

    public String greet() {
        return "Hello from " + platform.getName();
    }
}
