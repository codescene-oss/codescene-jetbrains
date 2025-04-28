Code Health Monitor [beta]

**NOTE:** _This feature is experimental and subject to change._

The Code Health Monitor shows any introduced issues among the files being worked on.
It compares the latest code review for a file with the review from the branch creation commit.
If the branch creation commit cannot be determined, the comparison falls back to the perfect code health score.
Any detected code health degradations, along with other issues or improvements, will be displayed in this view.