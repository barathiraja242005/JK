package com.barathiraja.jk.data

data class Article(val id: String, val title: String, val category: String, val minutes: Int, val body: List<String>)

/** Original JK guides. General fitness education, not medical advice. */
object Articles {
    val all = listOf(
        Article("progressive-overload", "Progressive overload: the one rule of getting stronger", "Training", 4, listOf(
            "Your body adapts to exactly what you ask of it. If you do the same workout with the same weight for months, you will mostly maintain — not improve.",
            "Progressive overload means gradually asking for a little more. Add one rep, a few seconds of hold time, a small amount of weight, or one extra set. Small changes compound.",
            "A simple method: pick a rep range like 8–12. When you can do 12 good reps on every set, increase the difficulty (more weight, a harder variation) and drop back to 8.",
            "Track your workouts. JK's history does this automatically for guided sessions, so you can see when you're ready to level up.",
        )),
        Article("protein", "How much protein do you actually need?", "Nutrition", 4, listOf(
            "Protein repairs and builds muscle, keeps you full and costs more energy to digest than carbs or fat.",
            "Active people generally do well with about 1.6–2.2 g of protein per kg of body weight per day. JK's macro target uses 1.6–2.0 g/kg depending on your goal.",
            "Spread it across the day: 20–40 g per meal is a practical target. Good vegetarian sources include paneer, curd, Greek yogurt, dal, soya chunks, tofu, chickpeas and milk.",
            "Non-vegetarian options like eggs, chicken breast and fish are dense in protein and low in fat. A whey or soy shake is a convenient top-up, not a requirement.",
        )),
        Article("fat-loss", "Fat loss without the myths", "Nutrition", 5, listOf(
            "Fat loss happens when you consistently eat a little less energy than you burn. No single food, supplement or time of day changes that rule.",
            "A moderate deficit of about 300–500 kcal a day is sustainable for most people and protects muscle — that's what JK's 'Lose fat' target uses.",
            "Keep protein high, lift weights or do bodyweight strength work to keep muscle, and walk more. Daily steps are one of the most underrated fat-loss tools.",
            "Expect the scale to bounce around with water and salt. Look at the weekly trend in JK's weight chart instead of single days.",
        )),
        Article("sleep", "Sleep: your secret recovery weapon", "Recovery", 3, listOf(
            "Muscles grow and repair while you rest, not while you train. Poor sleep increases hunger, lowers motivation and slows recovery.",
            "Aim for 7–9 hours. Keep a consistent schedule, dim screens an hour before bed and keep your room cool and dark.",
            "Try the 4-7-8 breathing pattern in JK's Breathe section to wind down at night.",
        )),
        Article("hydration", "Hydration basics", "Health", 3, listOf(
            "Even mild dehydration can make workouts feel harder and cause headaches and fatigue.",
            "A rough guide is around 35 ml per kg of body weight per day, more in hot weather or when you sweat a lot. JK sets your glass goal from your weight.",
            "Pale yellow urine is a simple check. Spread water through the day rather than drinking it all at once.",
        )),
        Article("warmup", "Why you should never skip the warm-up", "Training", 3, listOf(
            "A warm-up raises body temperature, lubricates joints and prepares your nervous system, making the main workout safer and more effective.",
            "Five minutes is enough: light cardio, dynamic movements like arm circles and inchworms, and a few easy reps of your first exercise.",
            "JK includes a 5-Minute Warm-Up program in the Home section — run it before any session.",
        )),
        Article("form", "Squat, push-up, plank: getting the form right", "Training", 5, listOf(
            "Squat: feet about shoulder-width, chest up, push hips back and down, knees track over toes, drive up through the whole foot.",
            "Push-up: hands under shoulders, body in one straight line from head to heels, lower the chest close to the floor, elbows at roughly 45° from the body.",
            "Plank: elbows under shoulders, squeeze glutes, brace abs as if about to be poked in the stomach, don't let hips sag or pike.",
            "Every exercise in JK has step-by-step photos and a 'Watch tutorial' button that opens video demonstrations on YouTube.",
        )),
        Article("fasting", "Intermittent fasting: is it for you?", "Nutrition", 4, listOf(
            "Intermittent fasting limits eating to a time window, such as 8 hours (16:8). For many people it simply makes eating less easier.",
            "It's not magic: total calories and food quality still matter most. Some people feel great; others get very hungry and overeat later.",
            "Start with a gentle 12–13 hour overnight fast using JK's fasting timer, and stretch it only if it feels good. Skip fasting if you're pregnant, diabetic or have a history of disordered eating.",
        )),
        Article("steps", "10,000 steps? What the research says", "Health", 3, listOf(
            "The 10,000-step number started as a marketing slogan, but more daily walking is consistently linked with better health.",
            "Benefits start well below 10,000 — gains are steep from 4,000 up to about 8,000 steps a day, then level off.",
            "Set a step goal slightly above your current average in JK's settings and raise it over time.",
        )),
        Article("stress", "Breathing your way out of stress", "Mind", 3, listOf(
            "Slow breathing with a longer exhale activates your parasympathetic nervous system — the 'rest and digest' mode.",
            "Box breathing (4-4-4-4) is used by athletes and first responders to stay calm under pressure.",
            "Even 2–3 minutes helps. Use JK's Breathe and Meditate sections whenever you feel overwhelmed.",
        )),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}
