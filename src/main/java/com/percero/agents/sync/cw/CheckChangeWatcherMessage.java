package com.percero.agents.sync.cw;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

/**
 * Created by jonnysamps on 11/23/15.
 */
public class CheckChangeWatcherMessage {
    public ClassIDPair classIDPair;
    public String[] fieldNames;
    public String[] params;
    public String oldValueJson;
}
