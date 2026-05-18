window.BENCHMARK_DATA = {
  "lastUpdate": 1779103664307,
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
          "id": "457a79810a075928d027f6b1272a3f3050007cbc",
          "message": "Merge pull request #143 from codescene-oss/fix-may13\n\nFix review queue race condition by consolidating into CodeReviewer",
          "timestamp": "2026-05-13T13:33:00+02:00",
          "tree_id": "f78702d27686979abfe24425bb92c4408450a856",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/457a79810a075928d027f6b1272a3f3050007cbc"
        },
        "date": 1778672807133,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.467793736984316,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.453149220865953,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8415196525600948,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.193643758910805,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7234952802493229,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9974023761655169,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.50497039809451,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.845183494443735,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.852534392215706,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.406458978593054,
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
          "id": "398b6a9c800e12f5d278c55699519620792808d7",
          "message": "Merge pull request #144 from codescene-oss/slash-ui\n\nNormalize editor paths in delta analysis for consistent UI display",
          "timestamp": "2026-05-14T15:40:11+02:00",
          "tree_id": "c2161eef1c914c4e604ba9ad17d890c9aaaefcde",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/398b6a9c800e12f5d278c55699519620792808d7"
        },
        "date": 1778766835400,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 12.091184543425282,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.170756772020081,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.860101841485248,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.1921339543296252,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7652708553572805,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.984529745930472,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.195471101948346,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.099895249998478,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 18.422847332103366,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.921025660538948,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "vemv@users.noreply.github.com",
            "name": "vemv",
            "username": "vemv"
          },
          "committer": {
            "email": "vemv@users.noreply.github.com",
            "name": "vemv",
            "username": "vemv"
          },
          "distinct": true,
          "id": "40bdba574c02da985c39f38ff82eaa1be054c8ce",
          "message": "Fix Makefile creating literal NUL files in MSYS environment\n\nUse /dev/null for all platforms instead of Windows NUL device. MSYS/Git\nBash doesn't recognize NUL as a device name and creates literal files.",
          "timestamp": "2026-05-15T04:39:23+02:00",
          "tree_id": "924a43beed60977dc201c85219226c35b7abc19b",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/40bdba574c02da985c39f38ff82eaa1be054c8ce"
        },
        "date": 1778813587572,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.517772073203524,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.258876700044336,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8470963406507448,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.18834942325737894,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.9671894278400122,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0385343812099808,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.765361225518399,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.248922359535652,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 18.8245118774908,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.595565756667385,
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
          "id": "fc17f3ec3e47cbef90a53680ef4dcfe3d9bdb67e",
          "message": "fix: CS-6901: closest main-line merge base for baseline (#145)\n\n* fix(CS-6901): pick closest main-line merge base for baseline\n\nShare selection in core, wire Git4IdeaGitService and Git4IdeaChangeLister,\nadd merge-base --is-ancestor on GitCommandExecutor, align test doubles.\nSplit Git4IdeaChangeListerTest to satisfy class-size limit.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-18T12:13:45+01:00",
          "tree_id": "02aeb37e2c9e38a297ee131e34357b86a40f4953",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/fc17f3ec3e47cbef90a53680ef4dcfe3d9bdb67e"
        },
        "date": 1779103663330,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.911642256844578,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.063328016285199,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.831721597780988,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2149072339641092,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7395348468791407,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9905131404216139,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.5858520137734,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.734988029685687,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.79405413457069,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.748131832599332,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}