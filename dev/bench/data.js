window.BENCHMARK_DATA = {
  "lastUpdate": 1778068811225,
  "repoUrl": "https://github.com/codescene-oss/codescene-jetbrains",
  "entries": {
    "JetBrains Plugin - ExtensionAPI": [
      {
        "commit": {
          "author": {
            "name": "codescene-oss",
            "username": "codescene-oss"
          },
          "committer": {
            "name": "codescene-oss",
            "username": "codescene-oss"
          },
          "id": "f90215593e698f0e157b878ab7f253563ef46e2c",
          "message": "Add ExtensionAPI benchmarks",
          "timestamp": "2026-05-05T08:22:25Z",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/pull/141/commits/f90215593e698f0e157b878ab7f253563ef46e2c"
        },
        "date": 1778068809414,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.51616795611985,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.103890753265112,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.853648225337912,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.1923203538807539,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7307136301972093,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.954593400780454,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.149660122280192,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.87964133756375,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.62594309547177,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.776440679793188,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}