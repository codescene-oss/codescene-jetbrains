package com.codescene.jetbrains.core.git

class WorkspaceActivity {
    private var activitySinceLastScan = false

    fun markWorkspaceFileActivity() {
        activitySinceLastScan = true
    }

    fun consumeWorkspaceFileActivity(): Boolean {
        val hadActivity = activitySinceLastScan
        activitySinceLastScan = false
        return hadActivity
    }

    fun reset() {
        activitySinceLastScan = false
    }
}
