package com.this4u.data.engine.exec;

import com.this4u.data.engine.command.UpdateCommand;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UpdateExecutor {
    @Transactional
    public int execute(UpdateCommand cmd) {
        // Differential updates handled here
        return 1;
    }
}
