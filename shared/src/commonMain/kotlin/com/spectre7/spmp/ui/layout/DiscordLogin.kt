package com.spectre7.spmp.ui.layout

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.api.*
import com.spectre7.spmp.platform.WebViewLogin
import com.spectre7.spmp.platform.composable.rememberImagePainter
import com.spectre7.spmp.platform.isWebViewLoginSupported
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.catchInterrupts
import com.spectre7.utils.composable.Marquee
import com.spectre7.utils.composable.SubtleLoadingIndicator
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import kotlin.concurrent.thread

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val DISCORD_API_URL = "https://discord.com/api/"
private const val DISCORD_DEFAULT_AVATAR = "https://discord.com/assets/1f0bfc0865d324c2587920a7d80c609b.png"

@Composable
fun DiscordLogin(modifier: Modifier = Modifier, onFinished: (Result<String?>?) -> Unit) {
    if (isWebViewLoginSupported()) {
        WebViewLogin(DISCORD_LOGIN_URL, modifier, { it.startsWith(DISCORD_LOGIN_URL) }) { request, openUrl, getCookie ->
            if (request.url.startsWith(DISCORD_API_URL)) {
                val auth = request.requestHeaders["Authorization"]
                if (auth != null) {
                    onFinished(Result.success(auth))
                }
            }
        }
    }
    else {
        // TODO
        SpMp.context.openUrl(DISCORD_LOGIN_URL)
    }
}

private data class DiscordMeResponse(
    val id: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val discriminator: String? = null,
    val banner_color: String? = null,
    val bio: String? = null
) {
    var token: String? = null

    fun isEmpty(): Boolean = this == EMPTY
    fun getAvatarUrl(): String {
        check(!isEmpty())
        check(id != null)

        return if (avatar != null) "https://cdn.discordapp.com/avatars/$id/$avatar.webp"
                else DISCORD_DEFAULT_AVATAR
    }

    companion object {
        val EMPTY = DiscordMeResponse()
    }
}

private fun getDiscordAccountInfo(account_token: String): Result<DiscordMeResponse> {
    val request = Request.Builder()
        .url("https://discord.com/api/v9/users/@me")
        .addHeader("authorization", account_token)
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val response = result.getOrThrow()
    val me: DiscordMeResponse = Klaxon().parse(response.body!!.charStream())!!
    me.token = account_token

    response.close()
    return Result.success(me)
}

private val DiscordMeResponseSaver = run {
    mapSaver(
        save = {
            if (it.isEmpty()) emptyMap()
            else with (it) { mapOf(
                "id" to id,
                "username" to username,
                "avatar" to avatar,
                "discriminator" to discriminator,
                "banner_color" to banner_color,
                "bio" to bio,
                "token" to token
            )}
        },
        restore = { Klaxon().parseFromJsonObject<DiscordMeResponse>(JsonObject(it))?.apply { token = it["token"] as String? } }
    )
}

@Composable
fun DiscordAccountPreview(account_token: String, modifier: Modifier = Modifier) {
    var load_thread: Thread? by remember { mutableStateOf(null) }
    var me by rememberSaveable(stateSaver = DiscordMeResponseSaver) { mutableStateOf(DiscordMeResponse.EMPTY) }
    var started by remember { mutableStateOf(false) }

    DisposableEffect(account_token) {
        load_thread?.interrupt()

        if (me.token != account_token) {
            load_thread = thread {
                catchInterrupts {
                    me = getDiscordAccountInfo(account_token).getOrReport("DiscordAccountPreview") ?: DiscordMeResponse.EMPTY
                    load_thread = null
                }
            }

            me = DiscordMeResponse.EMPTY
            started = true
        }

        onDispose {
            load_thread?.interrupt()
        }
    }

    Crossfade(if (!me.isEmpty()) me else if (started) load_thread != null else null, modifier.fillMaxHeight()) { state ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state == true) {
                SubtleLoadingIndicator(Modifier.fillMaxSize())
            }
            else if (state == false) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Error, null)
                }
            }
            else if (state is DiscordMeResponse) {
                Image(rememberImagePainter(state.getAvatarUrl()), null, Modifier.fillMaxHeight().aspectRatio(1f).clip(CircleShape))

                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                    Text(state.username!!, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    Text("#${state.discriminator}")
                }
            }
        }
    }
}