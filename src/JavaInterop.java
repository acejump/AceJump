import com.intellij.util.EventDispatcher;

import javax.swing.event.ChangeListener;
import java.util.EventListener;

public class JavaInterop {
    public static EventDispatcher<ChangeListener> createChangeListener(){
        return EventDispatcher.create(ChangeListener.class);
    }
}
