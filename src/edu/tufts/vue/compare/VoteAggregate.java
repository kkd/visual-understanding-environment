/*
 * VotingAggregate.java
 *
 * Created on October 13, 2006, 5:41 PM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.compare;

import java.util.*;

public class VoteAggregate extends WeightAggregate {
    public static final int POSITIVE_VOTE = 1;
    public static final int NEGATIVE_VOTE = 0;
    /** Creates a new instance of VotingAggregate */
     public VoteAggregate(List<ConnectivityMatrix> matrices ){
         super(matrices);
         computeVotes();
    }
    
    private void computeVotes() {
        int count = getCount();
        float threshold = (float)count/2;
        for(int i=0; i<size;i++){
            for(int j=0;j<size;j++) {
                if(c[i][j] >= threshold) {
                    c[i][j] = POSITIVE_VOTE;
                } else {
                    c[i][j] = NEGATIVE_VOTE;
                }
            }
        }
    }
}
