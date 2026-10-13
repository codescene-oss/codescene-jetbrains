package com.codescene.jetbrains.core.cli

data class RepoLocation(
    val repoRoot: String,
    val relPath: String,
)

class WorkspaceReviewSubmitter(
    private val pipeline: WorkspaceReviewPipeline,
    private val locate: (String) -> RepoLocation?,
    private val openText: (String) -> String?,
    private val readBytes: (String) -> ByteArray?,
    private val isSupported: (String) -> Boolean,
) {
    fun submitEditor(
        absolutePath: String,
        content: String,
    ) {
        if (!isSupported(absolutePath)) return
        val location = locate(absolutePath) ?: return
        pipeline.submit(
            location.repoRoot,
            listOf(BufferSubmission(absolutePath, location.relPath, content)),
            emptyList(),
        )
    }

    fun submitPaths(paths: List<String>) {
        val buffersByRepo = mutableMapOf<String, MutableList<BufferSubmission>>()
        val disksByRepo = mutableMapOf<String, MutableList<DiskSubmission>>()
        paths.forEach { queuePath(it, buffersByRepo, disksByRepo) }
        submitGrouped(buffersByRepo, disksByRepo)
    }

    private fun queuePath(
        path: String,
        buffersByRepo: MutableMap<String, MutableList<BufferSubmission>>,
        disksByRepo: MutableMap<String, MutableList<DiskSubmission>>,
    ) {
        if (!isSupported(path)) return
        val location = locate(path) ?: return
        val open = openText(path)
        if (open != null) {
            buffersByRepo
                .getOrPut(location.repoRoot) { mutableListOf() }
                .add(BufferSubmission(path, location.relPath, open))
            return
        }
        val bytes = readBytes(path) ?: return
        disksByRepo
            .getOrPut(location.repoRoot) { mutableListOf() }
            .add(
                DiskSubmission(
                    absolutePath = path,
                    relPath = location.relPath,
                    gitBlobSha = GitBlobSha.ofBytes(bytes),
                ),
            )
    }

    private fun submitGrouped(
        buffersByRepo: Map<String, List<BufferSubmission>>,
        disksByRepo: Map<String, List<DiskSubmission>>,
    ) {
        for (repoRoot in buffersByRepo.keys + disksByRepo.keys) {
            val buffers = buffersByRepo[repoRoot] ?: emptyList()
            val disks = disksByRepo[repoRoot] ?: emptyList()
            if (buffers.isEmpty() && disks.isEmpty()) continue
            pipeline.submit(repoRoot, buffers, disks)
        }
    }
}
