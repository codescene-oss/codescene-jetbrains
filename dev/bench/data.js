window.BENCHMARK_DATA = {
  "lastUpdate": 1781183161498,
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
          "id": "c1242ddf5afa06b3993570ff79de26db3d8b7f56",
          "message": "fix: restore dynamic plugin install (#147)\n\nRemove the explicit restart requirement from the plugin descriptor and add a regression test so dynamic install support does not regress.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-18T14:49:48+01:00",
          "tree_id": "ced89f4d212e055bba9b4e9afe28c71e58df1b70",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/c1242ddf5afa06b3993570ff79de26db3d8b7f56"
        },
        "date": 1779113054721,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.943565486898786,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.030110117091359,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8209718385944988,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2134664796783376,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7834652292977917,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0165715289047252,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.845708839084123,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.660962803077692,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.22070586529217,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.7786704721267,
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
          "id": "8ae2f6c57cc681e409790a4789651ef338400647",
          "message": "fix: guard stale ACE code vision ranges and skip unsupported review file types (#148)\n\n* fix: guard stale code vision ranges\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* fix: skip unsupported file types before review API calls\n\nAlign git change detection with isSupportedLanguage and guard CachedReviewService entry points so .gradle and other unsupported extensions never reach the Extension API.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-18T21:24:51+01:00",
          "tree_id": "adc681677a3d0434c99a1351428f0db1ea95aae2",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/8ae2f6c57cc681e409790a4789651ef338400647"
        },
        "date": 1779136725020,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.415218144437514,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.138135469648583,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.857379382567892,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.193649812043593,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7466326116359425,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9733278347222859,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.47661645259871,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.824978066197742,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.317560801280166,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.15066933783641,
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
          "id": "07d870652f5dc1de6c86978ca10beb5ad5a06683",
          "message": "Delete .cursor/plans/fix_cs-6901_41bc957c.plan.md",
          "timestamp": "2026-05-19T08:22:32+02:00",
          "tree_id": "e59d6ca883e2c77f0f31fb796b22c0729c4cf914",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/07d870652f5dc1de6c86978ca10beb5ad5a06683"
        },
        "date": 1779172570270,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.024247627374326,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.1923522449106105,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.834041421609328,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.21163444294305406,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7351342065319717,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9965662416886476,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.356269145847643,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.882006386337589,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.59376961604203,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.6508838972731,
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
          "id": "8bf4d09d871f333b7bd451b59df7751eb34e8d1f",
          "message": "Fix flaky refactorable-fn in Code Health Monitor (#149)\n\n* Fix flaky refactorable-fn in Code Health Monitor\n\nAlign monitor ACE lookup with Code Vision stale fallback, normalize cache\npath keys, refresh the monitor after ACE cache writes, and populate ACE on\npath-based reviews triggered by git change observation.\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-19T13:04:13+02:00",
          "tree_id": "b32a9d0973e7009a46f2127ccf41d08f1f2329cd",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/8bf4d09d871f333b7bd451b59df7751eb34e8d1f"
        },
        "date": 1779189507572,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.846583911464965,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.829114925263422,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7791336472673442,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.230504327563046,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7092860956499538,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9787604533377172,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.416703763216672,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.566989600669435,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.474199553454383,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.285310549629738,
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
          "id": "b873801b6224f1610e6430a70cf120dde3ae1227",
          "message": "fix: avoid synchronous git repository update on EDT (#150)\n\n* fix: avoid synchronous git repository update on EDT\n\nGit change observer scheduled reviews on invokeLater, which called getRepositoryForFile on the EDT at project open and logged 'Do not call synchronous repository update in EDT'.\n\nSchedule reviews on CachedReviewService IO scope with ReadAction for editor lookup, and resolve repositories via getRepositoryForRootQuick on the EDT.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* fix: resolve nested git repos to deepest matching root on EDT\n\nWhen getRepositoryForRootQuick misses, pick the longest repo root prefix instead of the first match so submodule/nested repos win for relative path and ignore checks.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-19T16:49:48+02:00",
          "tree_id": "26db877a9471f1162eeba745348384d4899ee982",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/b873801b6224f1610e6430a70cf120dde3ae1227"
        },
        "date": 1779203008626,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.02365092841217,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.047937802721514,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8348877735555267,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2106299960188731,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7270406208637232,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0334904834103706,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.205150470485084,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.60848512969898,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.98981155710368,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.38627533623234,
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
          "id": "9d467b1c6272287b156f59d3949b716fe1bb1f6b",
          "message": "fix: avoid synchronous git under ReadAction during file support check (#151)\n\nUnify resolveRepository to always use getRepositoryForRootQuick and\ndeepest-repo fallback instead of getRepositoryForFile off the EDT, which\ncould spawn git config under a read lock. Narrow ReadAction in\nFileEditorLifecycleListener to document text only.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-20T12:20:57+02:00",
          "tree_id": "87743e0d985a99be3d58cf93dc0de4d806586888",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/9d467b1c6272287b156f59d3949b716fe1bb1f6b"
        },
        "date": 1779273285314,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 13.277502778411105,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 7.276213510201103,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8919395247706547,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2111521588177983,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 2.0118716969040937,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0892641507664518,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 10.387701112932103,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.5626097535045576,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 21.316280926894265,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 14.635556349697485,
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
          "id": "f6f395b4f0a797a0f07c7d0a566a10b149d78fb6",
          "message": "Fix: prefer editor buffer content to prevent Code Health Monitor flickering (#152)\n\n* Add support for 8 additional file extensions\n\nAdds Elixir (.ex, .exs), Python stub files (.pyi), VisualForce (.efx, .emx),\nTcl (.tcl), and additional C++ variants (.c++, .pcc) for parity with VSCode\nand Visual Studio extensions.\n\n* fix: use editor buffer content to prevent Code Health Monitor flickering\n\nPrefer document content from editor buffers over disk content when available\nfor ACE cache lookups and reviews. This prevents the monitor from flip-flopping\nbetween fixed/unfixed states when a file has unsaved changes, as editor-triggered\nreviews and periodic polling now use the same content source.\n\n* refactor: introduce FileDataWithContent to reduce function arguments\n\nGroup FileMetaType with bufferContent into a single data class, reducing\nparameter count in resolveRefactoringRequest (7→6) and\nfetchRefactorableFunctionFromCache (5→4). Adds test coverage for the\nbuffer content branch.\n\n---------\n\nCo-authored-by: vemv <vemv@users.noreply.github.com>",
          "timestamp": "2026-05-25T10:09:31+02:00",
          "tree_id": "be20a4b5dfff5b8870b65f5dc8822b8d351e682e",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/f6f395b4f0a797a0f07c7d0a566a10b149d78fb6"
        },
        "date": 1779697401721,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.458235309298455,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.862005015369827,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7628155371946967,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2445346301434884,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7018973648173241,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9766154625973765,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.381309610710991,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.528939933067658,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.47027028890681,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.340425922250294,
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
          "id": "ac405c88a48323694efb8ed693a6b3038c50c4ba",
          "message": "fix: startup crash (#154)",
          "timestamp": "2026-05-25T16:27:20+02:00",
          "tree_id": "8722598d961bd26112cfab8ea8cada5f83643e9d",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/ac405c88a48323694efb8ed693a6b3038c50c4ba"
        },
        "date": 1779720058715,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.79458612750019,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.311861342763909,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8020669345325462,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.21762256226296978,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7279949017602927,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9823778454339243,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.693722742085205,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 6.267672062255643,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 23.52020372349979,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 16.573689190426997,
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
          "id": "7b7b3ae71426122012c3c61f2f98c9dee289ccf5",
          "message": "chore: Update README.md (#155)",
          "timestamp": "2026-05-25T17:01:31+02:00",
          "tree_id": "40f221b5d77a9035166efa443ac71303edaf6449",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/7b7b3ae71426122012c3c61f2f98c9dee289ccf5"
        },
        "date": 1779722103449,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.493626754561127,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.314400345130736,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8497152150430058,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.19185336118131982,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.748028839273756,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9941372836542337,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.91777652429083,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.046903588273052,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 18.565224893566683,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.35475691734823,
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
          "id": "6d34f6ae14c88fc014dcbc52ed62208fd2b3c78f",
          "message": "fix: ACE Activation popup (#153)",
          "timestamp": "2026-05-25T17:16:52+02:00",
          "tree_id": "9b2f2f3d66a53af98c77b404e6d6cd7bed5d7dbb",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/6d34f6ae14c88fc014dcbc52ed62208fd2b3c78f"
        },
        "date": 1779723043555,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.250911624269012,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.349769274702227,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8608970504001676,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.19827054839728225,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7252518694368573,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9884023546123364,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.829071482849727,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.013513915133023,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.022946845957396,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.890011059070927,
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
          "id": "e63ecc61e89f070cdcb35979bac94eeb9eea8735",
          "message": "Merge pull request #156 from codescene-oss/periodic-cleanup\n\nImplement StaleItemCleanup",
          "timestamp": "2026-05-26T10:17:41+02:00",
          "tree_id": "44e5cd7c386c0c97883cf8159263ea36a31d9ab4",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/e63ecc61e89f070cdcb35979bac94eeb9eea8735"
        },
        "date": 1779784302248,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.790382217088945,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.914878335491986,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7972236441638583,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.20710719489394025,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.6998350957838226,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.003176485577041,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.508946217070626,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.786531695902893,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.63322306554587,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.128439880458396,
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
          "id": "6e01d91090e37a44ed2b0a52f4d5fbdd32cbb0aa",
          "message": "fix: restore ACE isStale when source file paths differ in format (#157)\n\nUse pathComparisonKey in the stale update pipeline so normalized cache paths match VirtualFile paths on Windows (forward vs backslash, drive letter casing).\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-05-27T09:28:14+02:00",
          "tree_id": "42ead0af7e563815155139b23fd9895134103c38",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/6e01d91090e37a44ed2b0a52f4d5fbdd32cbb0aa"
        },
        "date": 1779867729845,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.954080025171113,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.940985595069614,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.794320424311453,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.20222972339988915,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7253270044213225,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0160913379631702,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.809847895985007,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.6541765750980275,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.548340958544195,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.551476441106342,
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
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "distinct": true,
          "id": "41bb86cdac5141d6c96ce62011e6b2249d1dee04",
          "message": "chore(release): v0.5.0",
          "timestamp": "2026-05-27T09:43:53+02:00",
          "tree_id": "46992e4fd7e322defeaec5bd81d0342d01948bf4",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/41bb86cdac5141d6c96ce62011e6b2249d1dee04"
        },
        "date": 1779868660369,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.837074800498357,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.988326426011158,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7345667178253799,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2546726648469934,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.709038318929511,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9799060265865771,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.595407845586632,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.627008520339672,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.17529649359117,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.612003331505957,
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
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "distinct": true,
          "id": "c9933219e660858c1b74f145fc802ad3903bf54c",
          "message": "chore(release): v0.5.0",
          "timestamp": "2026-05-27T10:20:16+02:00",
          "tree_id": "1471238c36f0564da7c81fb4e4de603ca67c219e",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/c9933219e660858c1b74f145fc802ad3903bf54c"
        },
        "date": 1779870834348,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.90450629805583,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.211005344883156,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8508752189249968,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.18931498327102744,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7857295248013376,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9865503040484083,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.589838362284329,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.90985287505257,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.486433377835226,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.392934234839757,
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
          "id": "7840f3741b2304dd38f4ab9ffff7fa99a3774cec",
          "message": "fix: internal intellij platform call (#159)",
          "timestamp": "2026-06-01T08:46:06+02:00",
          "tree_id": "faeb3be57b647c0d637943bc2ba93ca538c8b63a",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/7840f3741b2304dd38f4ab9ffff7fa99a3774cec"
        },
        "date": 1780297185048,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.440177928699665,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.455937440004912,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8750236799556994,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.19736522521348482,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.8043582750934604,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9889322018925428,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.462633458652233,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.843057483742243,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.109394697163076,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.1113884851409,
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
          "id": "8296b8eca40075755c572bc63beb786428784ec6",
          "message": "CS-11200: Store ACE auth token in PasswordSafe (#160)\n\n* CS-11200: Store ACE auth token in PasswordSafe\n\nMove the ACE token out of codescene-settings.xml into PasswordSafe, persist only aceTokenConfigured, and migrate legacy plaintext values on load.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11200: Avoid PasswordSafe inside settings read action\n\nDo not call PasswordSafe from getState or loadState; persist aceTokenConfigured from memory and migrate legacy tokens on a background thread.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11200: Read ACE token off EDT and cover updateAceTokenConfigured\n\nFetch the token on a background thread before refactor entry; preload from CWF handlers. Add SettingsStateManagerTest for updateAceTokenConfigured.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-02T11:10:48+02:00",
          "tree_id": "cfdfa58f4165ec47953d9b3394a0f10287bd4d72",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/8296b8eca40075755c572bc63beb786428784ec6"
        },
        "date": 1780392279685,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.69585722455318,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.8254771299578305,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7868747365001947,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.23951606614724125,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.6750925772160787,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9909119335794475,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.520225126450203,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.549428736916426,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 15.876022598000176,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.51107066659172,
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
          "id": "96543fe2da0a47f9078e7f9d2cadd2ce10215e83",
          "message": "fix: Safely embed JSON and CSS in webview scripts (#164)\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-02T15:39:24+02:00",
          "tree_id": "a65a4c1af918fb527a4e80541575c1d1662edfa0",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/96543fe2da0a47f9078e7f9d2cadd2ce10215e83"
        },
        "date": 1780408409345,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 12.295169988884425,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.671026608588207,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.924014842911875,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.21104871638329442,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.941355557301972,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.021830988678635,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 10.008746101106633,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.461352942476725,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 19.050516949641473,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.782979679198615,
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
          "id": "9af1d514e3ea29557ebc6549f54b58460773e649",
          "message": "CS-11204: Tighten CWF webview Content-Security-Policy (#163)\n\n* CS-11204: Tighten CWF webview Content-Security-Policy\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11204: Use JsEmbedEscapes for context after CS-11206 rebase\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-02T16:11:36+02:00",
          "tree_id": "7f7ee9c572f91b201b5c12bc0e79a283d1ced579",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/9af1d514e3ea29557ebc6549f54b58460773e649"
        },
        "date": 1780410351592,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 12.025274384432937,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.43686111421017,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.924139960001974,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.14071864626297345,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.8340215174884082,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9962889926952678,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.673840516749015,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.172748295884761,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 18.41904123490945,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.50716379672792,
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
          "id": "c9dfdd5b127bb0bd901763d4db5307a431a34d8b",
          "message": "CS-11202: Restrict CWF bridge to project-local file paths (#161)\n\n* CS-11202: Restrict CWF bridge to project-local file paths\n\nReject goto, docs, and refactoring requests whose paths fall outside the IDE\nworkspace and Git repository roots before opening or reading files.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11202: Harden CWF path guard parsing and cache roots\n\nCatch InvalidPathException, resolve paths with toRealPath before\nisPathUnderRoot, and cache allowed roots per project with Git invalidation.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11202: Split CWF path guard to reduce cyclomatic complexity\n\nExtract input parsing and absolute/relative checks so isCwfLocalFilePathAllowed\nstays under CodeScene complexity threshold.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11202: Add Cwf path guard unit test coverage\n\nCover IOException/SecurityException, blank and invalid roots, and absolute\npaths; remove unreachable toAbsolutePath catch.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* Fix ktlint issues in CwfActionLogicTest\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* Use NUL in invalidRootForPathsGet for cross-platform Paths.get failure\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11202: Cover InvalidPathException branches in path guard tests\n\nTest parseCwfPath directly with NUL input; add consolidated invalid-root\ntest; drop Assume skips now that invalidRootForPathsGet is cross-platform.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-02T16:25:50+02:00",
          "tree_id": "df978650488c34da36fef3090389e58ed73c9e4a",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/c9dfdd5b127bb0bd901763d4db5307a431a34d8b"
        },
        "date": 1780411207132,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.965942000727612,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.084805170739733,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8000460575592219,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2003656892366449,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7159059632020892,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9975389114763873,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.780784726490726,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.635094585978275,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.348723093390994,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.610965545596455,
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
          "id": "79464406cfd0fdca93834b5d87b19891a52d82bd",
          "message": "CS-11215: Escape theme CSS font-family for safe embedding (#171)\n\n* CS-11215: Escape theme CSS font-family for safe embedding\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* CS-11215: Cover CSS escape branches in unit tests\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-02T16:59:27+02:00",
          "tree_id": "6fb6e2df1b5bf32ddc276a63eca7d2a464714334",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/79464406cfd0fdca93834b5d87b19891a52d82bd"
        },
        "date": 1780413222557,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.092458151446772,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.155397455012189,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8263787902762054,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.20488084005709148,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7892761288850854,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9914760438912463,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.315601635515986,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.743525952185031,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.609728440496397,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.252657321803904,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "rok.lenarcic@gmail.com",
            "name": "Rok Lenarčič",
            "username": "RokLenarcic"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "b56b4d177545082e96f3eb8ba15ae89bdbc1a241",
          "message": "Merge pull request #166 from codescene-oss/CS-11203\n\nCS-11203: validate CWF URLs by parsed host",
          "timestamp": "2026-06-02T18:04:20+02:00",
          "tree_id": "5cae12070262f15c0ae419c9520fdd049f709d66",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/b56b4d177545082e96f3eb8ba15ae89bdbc1a241"
        },
        "date": 1780417106799,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 12.335741452516135,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.84051700456626,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8618693459779199,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.19423524570484169,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7726783989190849,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0020473552572444,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 10.180736067672658,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.173335438999665,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 18.691285178292325,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 13.003394224281099,
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
          "id": "5211f0c4f9487153a3e1d3f83c5ff06c54139a98",
          "message": "CS-11212: Pin third-party GitHub Actions to commit SHAs (#172)",
          "timestamp": "2026-06-03T08:22:39+02:00",
          "tree_id": "01e874f430bbfb776e21509cfb819246ed0c5612",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/5211f0c4f9487153a3e1d3f83c5ff06c54139a98"
        },
        "date": 1780468561773,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.702603694594549,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.37078378084801,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.858642831630273,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.19310740284460026,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.787235424453776,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0104163225066296,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 9.517550147190793,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.839751224039173,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.064295204633048,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.952402605169336,
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
          "id": "9a08f701f558a4db58b412ec5cafcafac89d293c",
          "message": "fix: Improve baseline branch detection (origin/HEAD + .codescene/config.json) (#173)\n\n* CS-11401: Improve baseline branch detection via origin/HEAD and repo config.\n\nPort repo-aware main-line resolution from VS so delta baselines use the actual default branch, optional .codescene/config.json override, and watch config or rules changes for cache invalidation.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n* Address PR review: tracker repopulate, origin/HEAD guard, safe config read.\n\nClear the change tracker before repopulating, reject stripped origin/HEAD literal, wrap config file reads in IOException handling, and use computeIfAbsent for resolver cache.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n\n---------\n\nCo-authored-by: Cursor <cursoragent@cursor.com>",
          "timestamp": "2026-06-03T10:48:04+02:00",
          "tree_id": "a53ad147f65c1d66f3b7b11ba39d24d62dc38747",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/9a08f701f558a4db58b412ec5cafcafac89d293c"
        },
        "date": 1780477312244,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 11.336820344691937,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.16329765784155,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.8662152622132322,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.18895743721830124,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7460179171666668,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9677626469762723,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.886416784754578,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.71407245671527,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 17.011983048917394,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.769290529106495,
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
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "distinct": true,
          "id": "1ed91fff7ab526bba5d2355c2457f092ac52db56",
          "message": "chore: update readme",
          "timestamp": "2026-06-03T11:17:54+02:00",
          "tree_id": "fa3dbfccfa631558a161a71940586dcb5e6dbf57",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/1ed91fff7ab526bba5d2355c2457f092ac52db56"
        },
        "date": 1780479092175,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 12.903780519118786,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.810306674721578,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.9406463029911858,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.13922819615253676,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.8948786649917804,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 1.0113768644094112,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 10.241108892617635,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 5.452024750090537,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 19.11437216830023,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 12.80658991801323,
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
            "email": "martin.safsten@codescene.com",
            "name": "Martin Säfsten",
            "username": "martinsafsten-codescene"
          },
          "distinct": true,
          "id": "d43c2172fe3324ee5d0b7760ec862a711e380054",
          "message": "chore(release): v0.5.1",
          "timestamp": "2026-06-03T14:15:26+02:00",
          "tree_id": "2768e851e209528eafa1f1a5a1ee12a74c00b398",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/d43c2172fe3324ee5d0b7760ec862a711e380054"
        },
        "date": 1780489773394,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.889484954259808,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 5.8355892419478925,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.7870975877910174,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.24786138977958333,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.6848307633068647,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9700548669398055,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.57655016423011,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.5595834600398515,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 15.914857581805828,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.366290794626545,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "tobias.anderson@systemverification.com",
            "name": "Tobias Anderson",
            "username": "tde01065"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "80e22e8af6a17825815f9287bb81664d54959a2b",
          "message": "Make CodeScene tool window observable for UI tests (#158)\n\n* Make CodeScene tool window available during indexing\n\n* Add CWF initialization marker for UI tests\n\n* Make UI test diagnostics property check null-safe",
          "timestamp": "2026-06-11T14:52:09+02:00",
          "tree_id": "1ef70ea3389060d9bdfda3d8c8d5b5a337187a3e",
          "url": "https://github.com/codescene-oss/codescene-jetbrains/commit/80e22e8af6a17825815f9287bb81664d54959a2b"
        },
        "date": 1781183159788,
        "tool": "jmh",
        "benches": [
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaCold",
            "value": 10.990727812838191,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiDeltaBenchmark.deltaWarm",
            "value": 6.207506200112698,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorCold",
            "value": 1.826729566671214,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiFnToRefactorBenchmark.fnToRefactorWarm",
            "value": 0.2084171110541289,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewCold",
            "value": 1.7357110612740967,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.baselineReviewWarm",
            "value": 0.9886809040052915,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewCold",
            "value": 8.696637837834265,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ExtensionApiReviewBenchmark.reviewWarm",
            "value": 4.772414472567377,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowCold",
            "value": 16.28063460751679,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "com.codescene.jetbrains.benchmarks.ReviewDeltaFlowBenchmark.reviewDeltaFlowWarm",
            "value": 11.773016769020106,
            "unit": "ms/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}