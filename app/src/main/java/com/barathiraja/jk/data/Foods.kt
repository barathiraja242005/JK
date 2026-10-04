package com.barathiraja.jk.data

/** Approximate nutrition per typical serving. Values are estimates for logging, not lab data. */
data class Food(
    val name: String,
    val serving: String,
    val kcal: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val veg: Boolean = true,
)

object Foods {
    private fun f(name: String, serving: String, kcal: Int, p: Number, c: Number, fat: Number, veg: Boolean = true) =
        Food(name, serving, kcal, p.toFloat(), c.toFloat(), fat.toFloat(), veg)

    val all: List<Food> = listOf(
        // Indian staples
        f("Chapati / Roti", "1 medium (40g)", 120, 3.5, 20, 3),
        f("Phulka (no oil)", "1 piece (30g)", 80, 2.7, 16, 0.5),
        f("Paratha (plain)", "1 piece", 260, 5, 36, 10),
        f("Aloo paratha", "1 piece", 300, 6, 42, 12),
        f("Steamed rice", "1 cup cooked", 205, 4.3, 45, 0.4),
        f("Brown rice", "1 cup cooked", 215, 5, 45, 1.8),
        f("Jeera rice", "1 cup", 250, 4.5, 45, 6),
        f("Veg biryani", "1 plate (300g)", 450, 10, 65, 16),
        f("Chicken biryani", "1 plate (350g)", 600, 30, 70, 22, veg = false),
        f("Dal (toor/arhar)", "1 bowl (200g)", 180, 10, 26, 4),
        f("Dal makhani", "1 bowl", 330, 12, 30, 18),
        f("Rajma curry", "1 bowl", 240, 12, 34, 6),
        f("Chole / chana masala", "1 bowl", 270, 12, 38, 8),
        f("Sambar", "1 bowl", 140, 6, 20, 4),
        f("Rasam", "1 bowl", 60, 2, 9, 2),
        f("Idli", "2 pieces", 120, 4, 24, 0.5),
        f("Plain dosa", "1 medium", 170, 4, 28, 4),
        f("Masala dosa", "1 piece", 380, 7, 52, 15),
        f("Uttapam", "1 piece", 220, 6, 34, 6),
        f("Upma", "1 bowl (200g)", 250, 6, 38, 8),
        f("Poha", "1 plate (200g)", 270, 5, 45, 8),
        f("Medu vada", "2 pieces", 280, 8, 26, 16),
        f("Pongal", "1 bowl", 300, 8, 40, 12),
        f("Coconut chutney", "2 tbsp", 70, 1, 3, 6),
        f("Paneer butter masala", "1 bowl", 400, 16, 14, 32),
        f("Palak paneer", "1 bowl", 290, 14, 10, 22),
        f("Paneer (raw)", "100 g", 265, 18, 3.6, 20),
        f("Paneer tikka", "6 pieces", 300, 20, 8, 21),
        f("Mixed veg curry", "1 bowl", 160, 4, 16, 9),
        f("Aloo gobi", "1 bowl", 180, 4, 22, 9),
        f("Bhindi fry", "1 bowl", 150, 3, 12, 10),
        f("Curd / dahi", "1 cup (200g)", 120, 7, 9, 6),
        f("Greek yogurt", "170 g", 100, 17, 6, 0.7),
        f("Raita", "1 bowl", 90, 4, 7, 5),
        f("Buttermilk / chaas", "1 glass", 40, 2.5, 4, 1.5),
        f("Lassi (sweet)", "1 glass", 220, 7, 34, 6),
        f("Masala chai (milk, sugar)", "1 cup", 90, 3, 13, 3),
        f("Filter coffee", "1 cup", 80, 3, 10, 3),
        f("Samosa", "1 piece", 260, 4, 30, 14),
        f("Pav bhaji", "1 plate", 500, 11, 65, 22),
        f("Vada pav", "1 piece", 300, 6, 42, 12),
        f("Pani puri", "6 pieces", 200, 3, 30, 8),
        f("Dhokla", "4 pieces", 160, 6, 24, 4),
        f("Khichdi", "1 bowl", 250, 9, 40, 6),
        f("Chicken curry", "1 bowl", 300, 25, 8, 18, veg = false),
        f("Butter chicken", "1 bowl", 450, 28, 12, 32, veg = false),
        f("Tandoori chicken", "2 pieces (200g)", 330, 45, 4, 14, veg = false),
        f("Chicken breast (grilled)", "100 g", 165, 31, 0, 3.6, veg = false),
        f("Fish curry", "1 bowl", 250, 24, 6, 14, veg = false),
        f("Fish fry", "1 piece (100g)", 230, 22, 6, 13, veg = false),
        f("Mutton curry", "1 bowl", 380, 28, 6, 27, veg = false),
        f("Egg (boiled)", "1 large", 78, 6.3, 0.6, 5.3, veg = false),
        f("Egg omelette", "2 eggs", 190, 13, 1.5, 15, veg = false),
        f("Egg bhurji", "2 eggs", 220, 13, 4, 17, veg = false),
        f("Egg whites", "3 whites", 51, 11, 0.7, 0.2, veg = false),
        f("Prawns (cooked)", "100 g", 100, 24, 0, 0.3, veg = false),
        f("Gulab jamun", "2 pieces", 300, 4, 44, 12),
        f("Rasgulla", "2 pieces", 220, 4, 46, 2),
        f("Kheer", "1 bowl", 270, 7, 40, 9),
        f("Jalebi", "100 g", 380, 3, 60, 14),

        // Breakfast & grains
        f("Oats (cooked with water)", "1 cup", 160, 6, 27, 3),
        f("Oats with milk", "1 bowl", 280, 12, 40, 8),
        f("Muesli", "50 g", 190, 5, 33, 4),
        f("Cornflakes with milk", "1 bowl", 230, 8, 40, 4),
        f("Whole wheat bread", "2 slices", 160, 8, 28, 2),
        f("White bread", "2 slices", 150, 5, 28, 2),
        f("Peanut butter", "2 tbsp", 190, 8, 6, 16),
        f("Quinoa (cooked)", "1 cup", 222, 8, 39, 3.6),
        f("Sweet potato (boiled)", "1 medium", 115, 2, 27, 0.2),
        f("Potato (boiled)", "1 medium", 160, 4, 37, 0.2),
        f("Pasta (cooked)", "1 cup", 220, 8, 43, 1.3),
        f("Sprouts salad", "1 bowl", 150, 10, 22, 2),

        // Fruit & veg
        f("Banana", "1 medium", 105, 1.3, 27, 0.4),
        f("Apple", "1 medium", 95, 0.5, 25, 0.3),
        f("Orange", "1 medium", 62, 1.2, 15, 0.2),
        f("Mango", "1 cup sliced", 100, 1.4, 25, 0.6),
        f("Papaya", "1 cup", 60, 0.7, 15, 0.4),
        f("Watermelon", "2 cups", 90, 1.8, 22, 0.4),
        f("Grapes", "1 cup", 104, 1.1, 27, 0.2),
        f("Pomegranate", "1/2 fruit", 115, 2.3, 26, 1.6),
        f("Guava", "1 fruit", 37, 1.4, 8, 0.5),
        f("Dates", "3 pieces", 70, 0.5, 18, 0),
        f("Green salad", "1 bowl", 40, 2, 8, 0.3),
        f("Cucumber", "1 medium", 30, 1.3, 7, 0.2),
        f("Broccoli (steamed)", "1 cup", 55, 3.7, 11, 0.6),
        f("Carrot", "1 medium", 25, 0.6, 6, 0.1),

        // Protein & dairy
        f("Milk (toned)", "1 glass (250ml)", 150, 8, 12, 7.5),
        f("Milk (skimmed)", "1 glass (250ml)", 85, 8.5, 12, 0.2),
        f("Soy milk", "1 glass", 100, 7, 8, 4),
        f("Whey protein", "1 scoop (30g)", 120, 24, 3, 1.5),
        f("Tofu", "100 g", 76, 8, 1.9, 4.8),
        f("Soya chunks (cooked)", "50 g dry", 170, 26, 16, 0.3),
        f("Chickpeas (boiled)", "1 cup", 270, 15, 45, 4),
        f("Moong dal chilla", "2 pieces", 220, 12, 28, 6),
        f("Cheese slice", "1 slice", 70, 4, 1, 5.5),

        // Snacks & nuts
        f("Almonds", "10 pieces", 70, 2.6, 2.5, 6),
        f("Walnuts", "5 halves", 65, 1.5, 1.4, 6.5),
        f("Peanuts (roasted)", "30 g", 170, 7, 5, 14),
        f("Cashews", "10 pieces", 90, 3, 5, 7),
        f("Roasted chana", "30 g", 110, 6, 18, 2),
        f("Makhana (roasted)", "1 cup", 100, 3.5, 20, 0.5),
        f("Protein bar", "1 bar", 200, 20, 22, 7),
        f("Biscuits (Marie)", "4 pieces", 120, 2, 20, 3.5),
        f("Potato chips", "30 g", 160, 2, 15, 10),
        f("Dark chocolate", "2 squares (20g)", 110, 1.5, 9, 8),

        // Fast food & drinks
        f("Veg sandwich", "1 sandwich", 250, 8, 35, 9),
        f("Chicken sandwich", "1 sandwich", 350, 22, 35, 12, veg = false),
        f("Pizza slice (veg)", "1 slice", 270, 11, 33, 10),
        f("Burger (veg)", "1 burger", 400, 11, 50, 17),
        f("Chicken burger", "1 burger", 480, 24, 45, 22, veg = false),
        f("French fries", "medium", 360, 4, 47, 17),
        f("Maggi noodles", "1 pack", 350, 8, 50, 13),
        f("Veg fried rice", "1 plate", 400, 8, 60, 14),
        f("Hakka noodles", "1 plate", 420, 9, 62, 15),
        f("Momos (veg, steamed)", "6 pieces", 240, 7, 38, 6),
        f("Momos (chicken, steamed)", "6 pieces", 280, 15, 34, 9, veg = false),
        f("Cola", "330 ml can", 140, 0, 35, 0),
        f("Fresh lime soda (sweet)", "1 glass", 90, 0, 23, 0),
        f("Coconut water", "1 glass", 45, 1.7, 9, 0.5),
        f("Orange juice", "1 glass", 110, 2, 26, 0.5),
        f("Beer", "330 ml", 150, 1.6, 13, 0),
    )

    fun search(q: String, vegOnly: Boolean): List<Food> {
        val t = q.trim().lowercase()
        return all.filter { (!vegOnly || it.veg) && (t.isEmpty() || it.name.lowercase().contains(t)) }
    }
}

data class MealPlanDay(val breakfast: String, val lunch: String, val snack: String, val dinner: String)

/** Sample day plans by goal. Original examples; portions should be adjusted to the calorie target. */
object MealPlans {
    fun plan(goal: Goal, veg: Boolean): List<Pair<String, MealPlanDay>> = when (goal) {
        Goal.LOSE -> if (veg) listOf(
            "Day A" to MealPlanDay("Vegetable oats upma + 1 glass buttermilk", "2 phulkas, dal, bhindi sabzi, salad",
                "1 apple + 10 almonds", "Paneer tikka (100g) + sautéed veggies"),
            "Day B" to MealPlanDay("Moong dal chilla ×2 + mint chutney", "1 cup brown rice, rajma, cucumber raita",
                "Roasted makhana + green tea", "Palak tofu + 1 phulka + salad"),
        ) else listOf(
            "Day A" to MealPlanDay("3 egg-white omelette + 1 slice whole wheat toast", "Grilled chicken (120g), 1 cup rice, dal, salad",
                "Greek yogurt + berries", "Fish curry + steamed veggies"),
            "Day B" to MealPlanDay("2 boiled eggs + papaya", "Chicken salad bowl with chickpeas",
                "Coconut water + roasted chana", "Tandoori chicken + 1 phulka + salad"),
        )
        Goal.MAINTAIN -> if (veg) listOf(
            "Day A" to MealPlanDay("Poha with peanuts + chai", "3 rotis, dal, mixed veg, curd",
                "Sprouts salad", "Veg khichdi + raita"),
            "Day B" to MealPlanDay("2 idli + sambar + chutney", "Rice, sambar, poriyal, curd",
                "Banana + peanut butter toast", "Paneer bhurji + 2 rotis"),
        ) else listOf(
            "Day A" to MealPlanDay("Egg bhurji + 2 toasts", "Chicken curry, rice, salad",
                "Fruit bowl + nuts", "Grilled fish + quinoa + veggies"),
            "Day B" to MealPlanDay("Masala omelette + oats", "Egg curry + 3 rotis + salad",
                "Greek yogurt", "Chicken stir-fry + brown rice"),
        )
        Goal.GAIN -> if (veg) listOf(
            "Day A" to MealPlanDay("Oats with milk, banana & peanut butter", "4 rotis, paneer curry, dal, rice, curd",
                "Whey/soy shake + dates + nuts", "Soya chunk pulao + raita"),
            "Day B" to MealPlanDay("Aloo paratha ×2 + curd", "Rajma chawal + salad + lassi",
                "Peanut chikki + milk", "Chole + 3 rotis + paneer tikka"),
        ) else listOf(
            "Day A" to MealPlanDay("4-egg omelette + 2 toasts + milk", "Chicken biryani + raita",
                "Whey shake + banana + almonds", "Mutton curry + rice + salad"),
            "Day B" to MealPlanDay("Oats with milk & whey + boiled eggs", "Grilled chicken, rice, dal, veggies",
                "Peanut butter sandwich + milk", "Fish curry + rice + egg bhurji"),
        )
    }
}
