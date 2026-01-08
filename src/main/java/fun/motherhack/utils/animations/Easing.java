package fun.motherhack.utils.animations;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum Easing {
	LINEAR(x -> x),
	BOTH_SINE(x -> -(Math.cos(Math.PI * x) - 1) / 2),
	BOTH_CIRC(x -> x < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2),
	BOTH_CUBIC(x -> x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2),
	EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2),
	EASE_OUT_BACK(x -> 1 + 2.70158 * Math.pow(x - 1, 3) + 1.70158 * Math.pow(x - 1, 2)),
	EASE_OUT_CIRC(x -> Math.sqrt(1 - Math.pow(x - 1, 2))),
	EASE_OUT_CUBIC(x -> 1 - Math.pow(1 - x, 3)),
	SMOOTH_STEP(x -> -2 * Math.pow(x, 3) + (3 * Math.pow(x, 2))),
	//(-1, 0.2, 0.75, 1.5)
	MotherHack(x -> 3 * Math.pow(1 - x, 2) * x * (-0.2) + 3 * (1 - x) * Math.pow(x, 2) * 1.5 + Math.pow(x, 3));

	private final Function<Double, Double> function;

	public double apply(double arg) {
		return function.apply(arg);
	}
}
