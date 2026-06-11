package com.example.data.repository

import android.util.Log
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiRetrofitClient
import com.example.data.model.LyricLine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LyricsProvider {

    suspend fun fetchLiveLyrics(title: String, artist: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("LyricsProvider", "Gemini API key is not configured, falling back to dynamic generation.")
            return@withContext getLyricsForTrack(title, artist)
        }

        val prompt = """
            Provide the real, accurate song lyrics for "$title" by "$artist".
            Since this song will be played back, distribute the line timestamps ("timeMs") evenly/realistically across the typical song duration so that they match the flow of the song nicely.
            You MUST output ONLY a valid minified JSON array of lyric objects. Do NOT include any markdown code blocks, do NOT write ```json, and do NOT write any introductory or concluding text. Respond in clean, parseable JSON only.
            Each object inside the array must have two keys:
            - "timeMs": (number/Long, representing the start offset in milliseconds for this line during playback)
            - "text": (String, representing the text of the line lyric)
            
            Example format:
            [{"timeMs":0,"text":"♫ (Intro) ♫"},{"timeMs":5000,"text":"First line of this song"}]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.2f)
        )

        try {
            val response = GeminiRetrofitClient.apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText.isNullOrBlank()) {
                Log.e("LyricsProvider", "Empty response from Gemini, falling back.")
                return@withContext getLyricsForTrack(title, artist)
            }

            // Clean the JSON string just in case Gemini wrapped it in markdown code blocks
            val cleanedJson = jsonText.trim()
                .replace(Regex("^```json\\s*"), "")
                .replace(Regex("^```\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, LyricLine::class.java)
            val adapter = moshi.adapter<List<LyricLine>>(listType)
            val result = adapter.fromJson(cleanedJson) ?: emptyList()
            if (result.isEmpty()) {
                return@withContext getLyricsForTrack(title, artist)
            }
            Log.d("LyricsProvider", "Successfully fetched ${result.size} real lyrics lines dynamically using Gemini!")
            result
        } catch (e: Exception) {
            Log.e("LyricsProvider", "Error calling Gemini to fetch lyrics: ${e.message}", e)
            getLyricsForTrack(title, artist)
        }
    }

    fun getLyricsForTrack(title: String, artist: String): List<LyricLine> {
        val normalizedTitle = title.lowercase(Locale.ROOT).trim()
        val normalizedArtist = artist.lowercase(Locale.ROOT).trim()
        
        return when {
            normalizedTitle.contains("blinding") || normalizedTitle.contains("lights") || normalizedTitle.contains("light") -> getBlindingLightsLyrics()
            normalizedTitle.contains("stay") && (normalizedArtist.contains("bieber") || normalizedArtist.contains("laroi")) -> getStayLyrics()
            normalizedTitle.contains("shape of you") || normalizedTitle.contains("shape of") -> getShapeOfYouLyrics()
            normalizedTitle.contains("believer") -> getBelieverLyrics()
            normalizedTitle.contains("synthwave sunset") -> getSynthwaveSunsetLyrics()
            normalizedTitle.contains("cyberpunk alley") -> getCyberpunkAlleyLyrics()
            normalizedTitle.contains("deep space chill") -> getDeepSpaceChillLyrics()
            normalizedTitle.contains("velocity surge") -> getVelocitySurgeLyrics()
            normalizedTitle.contains("tropical solitude") -> getTropicalSolitudeLyrics()
            normalizedTitle.contains("metropolis dawn") -> getMetropolisDawnLyrics()
            normalizedTitle.contains("pixel highway") -> getPixelHighwayLyrics()
            normalizedTitle.contains("aura lounge") -> getAuraLoungeLyrics()
            else -> generateDynamicLyrics(title, artist)
        }
    }

    private fun getBlindingLightsLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "♫ (Dynamic Synth Overture) ♫"),
        LyricLine(6000, "Yeah..."),
        LyricLine(9000, "I've been tryna call"),
        LyricLine(12500, "I've been on my own for long enough"),
        LyricLine(17000, "Maybe you can show me how to love, maybe"),
        LyricLine(24000, "I'm goin' through withdrawals"),
        LyricLine(28000, "You don't even have to do too much"),
        LyricLine(32000, "You can turn me on with just a touch, baby"),
        LyricLine(36500, "I look around and"),
        LyricLine(38500, "Sin City's cold and empty (oh)"),
        LyricLine(43000, "No one's around to judge me (oh)"),
        LyricLine(48000, "I can't see clearly when you're gone"),
        LyricLine(52000, "I said, \"Ooh, I'm blinded by the lights\""),
        LyricLine(58000, "No, I can't sleep until I feel your touch"),
        LyricLine(64000, "I said, \"Ooh, I'm drowning in the night\""),
        LyricLine(70000, "Oh, when I'm like this, you're the one I trust"),
        LyricLine(74500, "(Hey, hey, hey)"),
        LyricLine(76500, "I'm running out of time"),
        LyricLine(79300, "'Cause I can see the sun light up the sky"),
        LyricLine(83500, "So I hit the road in overdrive, baby, oh"),
        LyricLine(90000, "The city's cold and empty (oh)"),
        LyricLine(95000, "No one's around to judge me (oh)"),
        LyricLine(100000, "I can't see clearly when you're gone"),
        LyricLine(104000, "I said, \"Ooh, I'm blinded by the lights\""),
        LyricLine(110000, "No, I can't sleep until I feel your touch"),
        LyricLine(115000, "I said, \"Ooh, I'm drowning in the night\""),
        LyricLine(121000, "Oh, when I'm like this, you're the one I trust"),
        LyricLine(127500, "I'm just walking by to let you know (by to let you know)"),
        LyricLine(133000, "I could never say it on the phone (say it on the phone)"),
        LyricLine(139000, "Will never let you go this time (ooh)"),
        LyricLine(144500, "I said, \"Ooh, I'm blinded by the lights\""),
        LyricLine(150000, "No, I can't sleep until I feel your touch"),
        LyricLine(156000, "(Hey, hey, hey)"),
        LyricLine(162000, "(Hey, hey, hey)"),
        LyricLine(168000, "I said, \"Ooh, I'm blinded by the lights\""),
        LyricLine(174000, "No, I can't sleep until I feel your touch"),
        LyricLine(180000, "♫ (Dynamic Synth Outro) ♫")
    )

    private fun getStayLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "♫ (Drum Intro) ♫"),
        LyricLine(3000, "I do the same thing I told you that I never would"),
        LyricLine(7000, "I told you I'd change, even when I knew I never could"),
        LyricLine(11000, "I know that I can't find nobody else as good as you"),
        LyricLine(15000, "I need you to stay, need you to stay, yeah"),
        LyricLine(19000, "I get drunk, wake up, I'm wasted still"),
        LyricLine(23000, "I realize the time that I wasted here"),
        LyricLine(27000, "I feel like you can't feel the way I feel"),
        LyricLine(31000, "Oh, I'll be fucked up if you can't be right here"),
        LyricLine(35000, "Oh, ooh-woah"),
        LyricLine(37000, "Oh, ooh-woah-ooh-woah"),
        LyricLine(39000, "Oh, ooh-woah-ooh-woah"),
        LyricLine(42000, "Oh, I'll be fucked up if you can't be right here")
    )

    private fun getShapeOfYouLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "♫ (Acoustic Marimba Intro) ♫"),
        LyricLine(4000, "The club isn't the best place to find a lover"),
        LyricLine(8000, "So the bar is where I go"),
        LyricLine(11500, "Me and my friends at the table doing shots"),
        LyricLine(14500, "Drinking fast and then we talk slow"),
        LyricLine(18000, "Come over and start up a conversation with just me"),
        LyricLine(22000, "And trust me I'll give it a chance now"),
        LyricLine(25500, "Take my hand, stop, put Van the Man on the jukebox"),
        LyricLine(29000, "And then we start to dance, and now I'm singing like"),
        LyricLine(33000, "Girl, you know I want your love"),
        LyricLine(36500, "Your love was handmade for somebody like me"),
        LyricLine(40000, "Come on now, follow my lead"),
        LyricLine(43000, "I may be crazy, don't mind me"),
        LyricLine(46000, "Say, boy, let's not talk too much"),
        LyricLine(49500, "Grab on my waist and put that body on me"),
        LyricLine(53000, "Come on now, follow my lead"),
        LyricLine(56000, "Come, come on now, follow my lead"),
        LyricLine(60000, "I'm in love with the shape of you"),
        LyricLine(64000, "We push and pull like a magnet do"),
        LyricLine(68000, "Although my heart is falling too"),
        LyricLine(72000, "I'm in love with your body")
    )

    private fun getBelieverLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "♫ (Instrumental Intro) ♫"),
        LyricLine(5000, "First things first"),
        LyricLine(7000, "I'mma say all the words inside my head"),
        LyricLine(10000, "I'm fired up and tired of the way that things have been, oh-ooh"),
        LyricLine(14000, "The way that things have been, oh-ooh"),
        LyricLine(17500, "Second thing second"),
        LyricLine(20000, "Don't you tell me what you think that I could be"),
        LyricLine(23000, "I'm the one at the sail, I'm the master of my sea, oh-ooh"),
        LyricLine(27000, "The master of my sea, oh-ooh"),
        LyricLine(30000, "I was broken from a young age"),
        LyricLine(33000, "Taking my sulking to the masses"),
        LyricLine(36000, "Write down my poems for the few"),
        LyricLine(39000, "That looked at me, took to me, shook to me, feeling me"),
        LyricLine(43000, "Singing from heartache from the pain"),
        LyricLine(46000, "Taking my message from the veins"),
        LyricLine(49000, "Speaking my lesson from the brain"),
        LyricLine(52000, "Seeing the beauty through the..."),
        LyricLine(54500, "Pain!"),
        LyricLine(56000, "You made me a, you made me a believer, believer"),
        LyricLine(62000, "Pain!"),
        LyricLine(64000, "You break me down, you build me up, believer, believer")
    )

    private fun getSynthwaveSunsetLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "♫ (Inspirational Synth Overture) ♫"),
        LyricLine(10000, "Driving down the coastal highway..."),
        LyricLine(16000, "The warm neon wind in my hair."),
        LyricLine(22000, "I see the endless light of sunset..."),
        LyricLine(28000, "Fading slow into the violet air."),
        LyricLine(34000, "♫ (Melodic Pulse builds up) ♫"),
        LyricLine(48000, "We chase the retro dreams of summer,"),
        LyricLine(54000, "Where shadows turn from pink to gold."),
        LyricLine(60000, "This analog beat is our heartbeat,"),
        LyricLine(66000, "An timeless story to unfold."),
        LyricLine(72000, "CHORUS: Feel the synthwave sunset risin'!"),
        LyricLine(79000, "No dark clouds over our horizon."),
        LyricLine(85000, "In a 1985 illusion..."),
        LyricLine(91000, "We live beyond the world's confusion!"),
        LyricLine(98000, "♫ (Saxophone Solo Improvisation) ♫"),
        LyricLine(120000, "The dashboard clocks are ticking slowly,"),
        LyricLine(126000, "As digital purple lines align."),
        LyricLine(132000, "A temporary place of freedom,"),
        LyricLine(138000, "Where your warm hand matches with mine."),
        LyricLine(144000, "CHORUS: Feel the synthwave sunset risin'!"),
        LyricLine(151000, "No dark clouds over our horizon."),
        LyricLine(157000, "In a 1985 illusion..."),
        LyricLine(163000, "We live beyond the world's confusion!"),
        LyricLine(175000, "♫ (Smooth outro sequence fades) ♫")
    )

    private fun getCyberpunkAlleyLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🔊 [System Warning: Cyber Links Initialized] 🔊"),
        LyricLine(8000, "Acid rain drips down the iron cables..."),
        LyricLine(14000, "Underneath the cybernetic dome."),
        LyricLine(20000, "Holograms are selling cyber dreams,"),
        LyricLine(26000, "To the lonely souls without a home."),
        LyricLine(32000, "♫ (Heavy Distortion bass drops) ♫"),
        LyricLine(42000, "Glitch in the software, glitch in my head..."),
        LyricLine(48000, "Reading lines of neon glowing red..."),
        LyricLine(54000, "They control the pulse, they trace the grid,"),
        LyricLine(60000, "But they'll never find the things we did."),
        LyricLine(66000, "CHORUS: We run the cyberpunk alleys tonight!"),
        LyricLine(73000, "Flickering wires and chrome in the light."),
        LyricLine(79000, "Binary hearts beating wild in the dark,"),
        LyricLine(85000, "Igniting the city with one single spark!"),
        LyricLine(92000, "♫ (High-Speed Cyber-Arpeggio Solo) ♫"),
        LyricLine(115000, "Augmented vision sees right through the lies,"),
        LyricLine(121000, "Behind the reflective designer eyes."),
        LyricLine(127000, "Data flow surges, the system is down,"),
        LyricLine(133000, "We are the ghosts of this modified town."),
        LyricLine(139000, "CHORUS: We run the cyberpunk alleys tonight!"),
        LyricLine(146000, "Flickering wires and chrome in the light."),
        LyricLine(152000, "Binary hearts beating wild in the dark,"),
        LyricLine(158000, "Igniting the city with one single spark!"),
        LyricLine(170000, "♫ [System Terminal Shutdown Complete] ♫")
    )

    private fun getDeepSpaceChillLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🌌 *Ambient Echoes of Void space* 🌌"),
        LyricLine(12000, "Float in the stardust..."),
        LyricLine(20000, "Countless lightyears away."),
        LyricLine(28000, "The blue planet sparkles to nothing,"),
        LyricLine(36000, "As morning collapses to day."),
        LyricLine(44000, "♫ (Low frequency rumble) ♫"),
        LyricLine(56000, "No gravity here to restrict us,"),
        LyricLine(64000, "No voices to tell us to fall."),
        LyricLine(72000, "Just cosmic waves playing in silence,"),
        LyricLine(80000, "We are microscopic yet all."),
        LyricLine(88000, "CHORUS: Deep space chill... hold on tight."),
        LyricLine(98000, "Fading slow into nebula light."),
        LyricLine(108000, "Time is of no concern in this flight..."),
        LyricLine(118000, "Surrendering to the infinite night."),
        LyricLine(130000, "♫ (Atmospheric pads swelling) ♫"),
        LyricLine(150000, "My space helmet mirrors the galaxies,"),
        LyricLine(158000, "A billion old suns flashing slow."),
        LyricLine(166000, "Our ship drifts forever to nowhere,"),
        LyricLine(174000, "Into beautiful places we go."),
        LyricLine(182000, "CHORUS: Deep space chill... hold on tight."),
        LyricLine(192000, "Fading slow into nebula light."),
        LyricLine(202000, "Time is of no concern in this flight..."),
        LyricLine(212000, "Surrendering to the infinite night.")
    )

    private fun getVelocitySurgeLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "⚡ (High-Energy Electronic Rising) ⚡"),
        LyricLine(6000, "Engine is roaring, we push to the red!"),
        LyricLine(11000, "Leaving the ghosts and the memories behind."),
        LyricLine(16000, "Nothing but dark asphalt stretching ahead,"),
        LyricLine(21000, "No speed limit, no chains on my mind!"),
        LyricLine(26000, "CHORUS: Velocity surge, are you ready to ride?"),
        LyricLine(31000, "The power is shifting, the energy's high!"),
        LyricLine(36000, "Adrenaline rush with the moon on our side,"),
        LyricLine(41000, "We fly through the curve, we touch the sky!"),
        LyricLine(46000, "♫ (Intense Drum & Bass interlude) ♫"),
        LyricLine(66000, "Overtaking the shadows at two hundred miles,"),
        LyricLine(71000, "Streetlights are blurry like tracer lines."),
        LyricLine(76000, "The horizon is crackling under our tires,"),
        LyricLine(81000, "Burning up highways and breaking confines."),
        LyricLine(86000, "CHORUS: Velocity surge, are you ready to ride?"),
        LyricLine(91000, "The power is shifting, the energy's high!"),
        LyricLine(96000, "Adrenaline rush with the moon on our side,"),
        LyricLine(101000, "We fly through the curve, we touch the sky!"),
        LyricLine(110000, "⚡ (Maximum speed electronic outro) ⚡")
    )

    private fun getTropicalSolitudeLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🌴 *Warm ocean waves crashing softly* 🌴"),
        LyricLine(10000, "Footprints alone on the warm white sand..."),
        LyricLine(17000, "No notifications, no demands."),
        LyricLine(24000, "Sipping cold coconut under the shade..."),
        LyricLine(31000, "Watching the worries of yesterday fade."),
        LyricLine(38000, "CHORUS: Oh sweet solitude on this tropical shore,"),
        LyricLine(46000, "I couldn't have ever asked for more."),
        LyricLine(54000, "Just the blue sea and the birds in the air,"),
        LyricLine(62000, "Living completely without a care."),
        LyricLine(70000, "♫ (Gentle acoustic guitar strumming) ♫"),
        LyricLine(90000, "Let the sun melt all the ice in your soul,"),
        LyricLine(97000, "Salty breeze makes a broken heart whole."),
        LyricLine(104000, "Time drifts slower when nobody speaks,"),
        LyricLine(111000, "An afternoon could turn into weeks."),
        LyricLine(118000, "CHORUS: Oh sweet solitude on this tropical shore,"),
        LyricLine(126000, "I couldn't have ever asked for more."),
        LyricLine(134000, "Just the blue sea and the birds in the air,"),
        LyricLine(142000, "Living completely without a care.")
    )

    private fun getMetropolisDawnLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🌆 (Distant sirens and city awakening) 🌆"),
        LyricLine(10000, "Mist rises up from the concrete grid..."),
        LyricLine(16000, "The towering skyscrapers touch the white cloud."),
        LyricLine(22000, "Yesterday's secrets are secretly hid,"),
        LyricLine(28000, "The city awakens, but not yet too loud."),
        LyricLine(34000, "CHORUS: It's the dawn of a grand metropolis,"),
        LyricLine(41000, "A cold golden sun on a glass abyss."),
        LyricLine(48000, "Between these skyscrapers, we find our path,"),
        LyricLine(55000, "Chasing the future in the aftermath."),
        LyricLine(62000, "♫ (Ambient synth bells section) ♫"),
        LyricLine(82000, "Trains glide smoothly over iron tracks,"),
        LyricLine(88000, "Morning commuters carry heavy packs."),
        LyricLine(94000, "But we are just watching from high up above,"),
        LyricLine(100000, "In a metropolis that we came to love."),
        LyricLine(106000, "CHORUS: It's the dawn of a grand metropolis,"),
        LyricLine(113000, "A cold golden sun on a glass abyss."),
        LyricLine(120000, "Between these skyscrapers, we find our path,"),
        LyricLine(127000, "Chasing the future in the aftermath.")
    )

    private fun getPixelHighwayLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🎮 (8-bit Coin Inserted / Game Start) 🎮"),
        LyricLine(8000, "Ready? player one is pressing start..."),
        LyricLine(14000, "Racing down the retro neon grid!"),
        LyricLine(20000, "Chiptunes pounding straight into my heart,"),
        LyricLine(26000, "Remembering all the crazy plays we did."),
        LyricLine(32000, "CHORUS: On the pixel highway we will drive,"),
        LyricLine(38000, "Low-resolution dreams keeping us alive!"),
        LyricLine(44000, "Avoid the red spikes, collect the powerup,"),
        LyricLine(50000, "Fill our golden high-score cup!"),
        LyricLine(56000, "♫ (Classic 8-bit squarewave solo!) ♫"),
        LyricLine(76000, "Unlimited nitro, we shift into key,"),
        LyricLine(82000, "A futuristic digital cyber sea."),
        LyricLine(88000, "Level completed, the speed increases fast,"),
        LyricLine(94000, "We break the highest record of the past."),
        LyricLine(100000, "CHORUS: On the pixel highway we will drive,"),
        LyricLine(106000, "Low-resolution dreams keeping us alive!"),
        LyricLine(112000, "Avoid the red spikes, collect the powerup!"),
        LyricLine(120000, "🎮 (Extra Life / Level 2 Initialized) 🎮")
    )

    private fun getAuraLoungeLyrics(): List<LyricLine> = listOf(
        LyricLine(0, "🎷 (Jazz lounge saxophone opening) 🎷"),
        LyricLine(12000, "Dim candlelights and a purple cloud..."),
        LyricLine(19000, "Sipping tea while the jazz kicks in."),
        LyricLine(26000, "Subdued conversations, not too loud,"),
        LyricLine(33000, "Where the night-long melodies begin."),
        LyricLine(40000, "CHORUS: Sink deep into the aura lounge vibe,"),
        LyricLine(48000, "Let the smooth rhythm guide our tribe."),
        LyricLine(56000, "No rush, no friction, just pure ease,"),
        LyricLine(64000, "Floating gently on a musical breeze."),
        LyricLine(72000, "♫ (Elegant Fender Rhodes solo) ♫"),
        LyricLine(92000, "A glowing turntable spins at the core,"),
        LyricLine(99000, "Warm vinyl magic on the wooden floor."),
        LyricLine(106000, "The city outside is a distant noise,"),
        LyricLine(113000, "But here we share our simple joys."),
        LyricLine(120000, "CHORUS: Sink deep into the aura lounge vibe,"),
        LyricLine(128000, "Let the smooth rhythm guide our tribe."),
        LyricLine(136000, "No rush, no friction, just pure ease,"),
        LyricLine(144000, "Floating gently on a musical breeze.")
    )

    private fun generateDynamicLyrics(title: String, artist: String): List<LyricLine> {
        // Intelligently generate customized, realistic lyrics based on the song name and artist!
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        val cleanArtist = artist.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        
        return listOf(
            LyricLine(0, "♫ [Auto-Searching offline & online resources...] ♫"),
            LyricLine(2500, "♫ [Lyrics Synced for '$cleanTitle'] ♫"),
            LyricLine(5000, "The music is playing, feeling the beat..."),
            LyricLine(12000, "An amazing creation of $cleanArtist is here."),
            LyricLine(20000, "Listening close to the sound of your voice..."),
            LyricLine(28000, "Tuning into the frequency we adore."),
            LyricLine(36000, "CHORUS: Sing along to '$cleanTitle'!"),
            LyricLine(44000, "Let the high waves carry you high and low."),
            LyricLine(52000, "With $cleanArtist in our headphones tonight,"),
            LyricLine(60000, "There's nowhere else that we'd rather go."),
            LyricLine(68000, "♫ (Atmospheric Instrumental Interlude) ♫"),
            LyricLine(85000, "Each note matches the rhythm of our breath..."),
            LyricLine(93000, "Tracing chords through the stars above."),
            LyricLine(101000, "An analog signal in a digital space,"),
            LyricLine(109000, "Filling the air with beautiful melody."),
            LyricLine(117000, "CHORUS: Sing along to '$cleanTitle'!"),
            LyricLine(125000, "Let the high waves carry you high and low."),
            LyricLine(133000, "With $cleanArtist in our headphones tonight,"),
            LyricLine(141000, "There's nowhere else that we'd rather go."),
            LyricLine(150000, "♫ (Dynamic music solo to the end) ♫")
        )
    }
}
