package base;

public enum Time {
	DAY {
		public long toDay() {
			return 0;
		}
	},
	HOUR {
		@Override
		public long toDay() {
			return 0;
		}
	};

	public abstract long toDay();
}
