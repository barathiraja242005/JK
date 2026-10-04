package com.barathiraja.jk.data

enum class Ambience(val label: String) { RAIN("Soft rain"), OCEAN("Ocean waves"), DRONE("Warm tone"), SILENCE("Silence") }

/** A guided session: spoken lines cued at seconds from the start. */
data class Meditation(
    val id: String,
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val ambience: Ambience,
    val script: List<Pair<Int, String>>,
)

object Meditations {
    val all = listOf(
        Meditation("calm5", "Quick Calm", "5 min · reset your nervous system", 5, Ambience.RAIN, listOf(
            0 to "Welcome. Find a comfortable position and gently close your eyes.",
            15 to "Take a slow breath in through your nose. And let it go through your mouth.",
            40 to "Let your shoulders drop. Unclench your jaw. Soften your hands.",
            75 to "Now just notice your breath. You don't need to change it. Simply notice it.",
            120 to "If your mind wanders, that's fine. Gently bring your attention back to the breath.",
            180 to "Feel the weight of your body, supported and still.",
            240 to "Take one deeper breath now. Notice how you feel.",
            285 to "When you're ready, open your eyes. Well done.",
        )),
        Meditation("focus10", "Deep Focus", "10 min · clear mind before work or study", 10, Ambience.DRONE, listOf(
            0 to "Sit upright, feet flat, and rest your hands on your lap.",
            20 to "Breathe in for four. Hold for four. Out for four. Hold for four.",
            60 to "Keep this steady rhythm. Let each breath sharpen your attention.",
            150 to "Pick one point of focus: the feeling of air at your nostrils.",
            270 to "Thoughts will come. Label them 'thinking', and return to the breath.",
            420 to "Notice the space between thoughts. Rest there.",
            540 to "Set an intention for the next hour. What is the one thing that matters?",
            585 to "Open your eyes. Carry this focus with you.",
        )),
        Meditation("sleep10", "Sleep Wind-Down", "10 min · body scan for bedtime", 10, Ambience.OCEAN, listOf(
            0 to "Lie down comfortably. Let your eyes close and your body grow heavy.",
            30 to "Bring your attention to your feet. Let them relax completely.",
            90 to "Move up to your calves and knees. Let any tension melt away.",
            150 to "Your thighs and hips. Heavy and relaxed.",
            210 to "Your belly rising and falling slowly with each breath.",
            280 to "Your chest, shoulders and arms. Soft and warm.",
            350 to "Your neck, your face, your forehead. Completely at ease.",
            430 to "Breathe in for four, hold for seven, and breathe out slowly for eight.",
            540 to "There's nothing left to do today. Let yourself drift into sleep.",
        )),
        Meditation("gratitude5", "Gratitude", "5 min · end the day on a high", 5, Ambience.DRONE, listOf(
            0 to "Settle in and take three slow breaths.",
            30 to "Think of one thing today that went well, however small.",
            90 to "Picture it clearly. Notice how it feels to remember it.",
            150 to "Now think of one person you're grateful for. Silently thank them.",
            220 to "Finally, thank your body for carrying you through today.",
            285 to "Take a final breath, and gently return.",
        )),
        Meditation("anxiety7", "Ease Anxiety", "7 min · grounding when things feel too much", 7, Ambience.RAIN, listOf(
            0 to "You're safe right now. Let's slow things down together.",
            20 to "Breathe in for four, and out for six. Longer out-breaths calm the body.",
            70 to "Notice five things you can feel: your feet, your seat, your hands, your clothes, the air.",
            140 to "Notice the sounds around you, near and far.",
            200 to "Anxious feelings are like waves. They rise, and they pass.",
            290 to "Keep breathing out slowly. Let your body know it can relax.",
            400 to "When you're ready, come back to the room. You handled this.",
        )),
        Meditation("timer", "Unguided Timer", "Silence with ambient sound", 10, Ambience.OCEAN, listOf(
            0 to "Begin.",
        )),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}
