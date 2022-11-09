package org.conffuzz.internal;

import edu.neu.ccs.prl.meringue.Replayer;
import edu.neu.ccs.prl.meringue.ZestFramework;

public class ConfFuzzFramework extends ZestFramework {
    @Override
    public Class<? extends Replayer> getReplayerClass() {
        return ConfFuzzReplayer.class;
    }

    public String getMainClassName() {
        return FuzzForkMain.class.getName();
    }

    @Override
    public String getCoordinate() {
        return "org.conffuzz:conffuzz-meringue-extension";
    }
}