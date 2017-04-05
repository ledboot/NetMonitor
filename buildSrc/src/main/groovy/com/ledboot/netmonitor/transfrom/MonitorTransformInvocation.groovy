package com.ledboot.netmonitor.transfrom

import com.android.build.api.transform.SecondaryInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider

class MonitorTransformInvocation implements TransformInvocation {
    TransformInvocation base

    MonitorTransformInvocation(TransformInvocation transformInvocation) {
        base = transformInvocation
    }

    @Override
    Context getContext() {
        return base.context
    }

    @Override
    Collection<TransformInput> getInputs() {
        Collection<TransformInput> monitorsInput = new HashSet<>();
        base.inputs.each {
            monitorsInput.add(new MonitorTransformInput(it))
        }
        return monitorsInput
    }

    @Override
    Collection<TransformInput> getReferencedInputs() {
        return base.referencedInputs
    }

    @Override
    Collection<SecondaryInput> getSecondaryInputs() {
        return base.secondaryInputs
    }

    @Override
    TransformOutputProvider getOutputProvider() {
        return base.outputProvider
    }

    @Override
    boolean isIncremental() {
        return base.incremental
    }
}