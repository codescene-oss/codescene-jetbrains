window.BENCHMARK_DATA = {
  "lastUpdate": 1778595922080,
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
      },
      {
        "commit": {
          "author": {
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "c447364bfe2a845b6848b2da0c11b52f96ba25ec",
          "message": "chore: add extensionapi benchmarks (#141)\n\n* Add ExtensionAPI benchmarks\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-06T15:10:41+02:00",
          "tree_id": "664adf058ae2aaccdc75bcb421a775c5f2268529",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/c447364bfe2a845b6848b2da0c11b52f96ba25ec"
        },
        "date": 1778073919800,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.913821599750209,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.969049947035016,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8241019155665694,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2081667737584759,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.715262894761295,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9909458057519636,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.837370148328404,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.656466729259111,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.944721990950505,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.785477104428377,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "319afb22d1066b3ffd7975116b692010eba2df0d",
          "message": "Delete .github/workflows/git-ai.yaml",
          "timestamp": "2026-05-12T10:51:28+02:00",
          "tree_id": "a1e19f301794fba5bdca13abcbce82afd989813a",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/319afb22d1066b3ffd7975116b692010eba2df0d"
        },
        "date": 1778576688372,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 8.747950648829107,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.0424577798422785,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.4464718620401729,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2181335773692555,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.299811297368906,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.8485092253322171,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 7.132984914553575,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.698648133389316,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 15.225847258220274,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 16.84988812701991,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "58f004c7a7a23af8bde545ae4d9f1d4bb4bd36a9",
          "message": "chore(deps): bump cli (#142)",
          "timestamp": "2026-05-12T12:36:45+02:00",
          "tree_id": "7bf95eba4372150239c2668e39d1eabf951cd174",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/58f004c7a7a23af8bde545ae4d9f1d4bb4bd36a9"
        },
        "date": 1778583061864,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.089391740497268,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.866965495422315,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7321205376417539,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.24218150325564752,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.6842113204125035,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.897198375987943,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.784738585225229,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.599145239143814,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.232001194992325,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.594001871540863,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "victor.valenzuela+gh@codescene.com",
            "name": "vemvcs",
            "username": "vemvcs"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "ceca63767a6521bdd20237e3a5cc101286098215",
          "message": "Merge pull request #135 from codescene-oss/integrate-GCO\n\nIntegrate GitChangeObserver",
          "timestamp": "2026-05-12T16:11:28+02:00",
          "tree_id": "a9af34eda1242d4124663b8fe3f979fbf558974a",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/ceca63767a6521bdd20237e3a5cc101286098215"
        },
        "date": 1778595920871,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.879678520910366,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 13.868174691603377,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.4500629957962698,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.9099364513111265,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.4768749211343353,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 5.413291299355879,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 7.7420424983869935,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 12.467254037655753,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 44.48157376020433,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 34.46327208190568,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}