package com.hwx.chatique.ui.communicationRoom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.hwx.chatique.R
import com.hwx.chatique.arch.extensions.equalsToOneOf
import com.hwx.chatique.flow.video.IVideoController
import com.hwx.chatique.network.models.ChatResponseItemGroupType
import com.hwx.chatique.ui.communicationRoom.stolen.AutoFitSurfaceView
import com.hwx.chatique.ui.communicationRoom.stolen.AutoFitTextureView
import kotlinx.coroutines.flow.map

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun CommunicationRoomScreen(
    state: CommunicationRoomContract.State,
    onEventSent: (event: CommunicationRoomContract.Event) -> Unit,
    videoController: IVideoController,
    onNavigationRequested: (navigationEffect: CommunicationRoomContract.Effect.Navigation) -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val (txOpponent, selfPreview, fps) = createRefs()

        if (state.isShowingOpponentCameraPreview) {
            // Adds view to Compose
            AndroidView(
                modifier = Modifier
                    .constrainAs(txOpponent) {

                    }
                    .fillMaxHeight()
                    .fillMaxWidth(),
                factory = { context ->
                    // Creates custom view
                    AutoFitSurfaceView(context)

                },
                update = { view ->
                    videoController.initOpponentPreview(view)
                    // View's been inflated or state read in this block has been updated
                }
            )

            val selfFps =
                videoController.selfFps.map { it.toString() }.collectAsState(initial = "").value
            val oppFps =
                videoController.opponentFps.map { it.toString() }.collectAsState(initial = "").value
            Text(
                text = "self cam fps = $selfFps \nopp fps = $oppFps",
                fontSize = 12.sp,
                color = Color.Yellow,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(0.dp, 16.dp, 16.dp, 0.dp)
                    .constrainAs(fps) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    },
            )
        }

        if (state.isShowingSelfCameraPreview) {
            // Adds view to Compose
            AndroidView(
                modifier = Modifier
                    .constrainAs(selfPreview) {
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                    .padding(0.dp, 0.dp, 16.dp, 16.dp)
                    .width(140.dp)
                    .height(140.dp),
                factory = { context ->
                    // Creates custom view
                    AutoFitTextureView(context)
                },
                update = { view ->
                    videoController.initSelfPreview(view)
                    // View's been inflated or state read in this block has been updated
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {

            Text(
                text = state.state.name,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.White),
            )

            if (state.state == CommunicationRoomContract.CallState.INCOMING) {

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painterResource(R.drawable.ic_call_accept),
                    "",
                    modifier = Modifier
                        .clickable {
                            onEventSent(CommunicationRoomContract.Event.OnAcceptCallClick)
                        }
                        .background(Color.White)
                        .size(42.dp),
                )
            }
            if (state.state.equalsToOneOf(
                    CommunicationRoomContract.CallState.INCOMING,
                    CommunicationRoomContract.CallState.OUTGOING,
                    CommunicationRoomContract.CallState.IN_PROCESS,
                )
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painterResource(R.drawable.ic_call_decline),
                    "",
                    modifier = Modifier
                        .clickable {
                            onEventSent(CommunicationRoomContract.Event.OnDeclineCallClick)
                        }
                        .background(Color.White)
                        .size(42.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val micRes = if (state.isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
            Image(
                painterResource(micRes),
                "",
                modifier = Modifier
                    .clickable {
                        onEventSent(CommunicationRoomContract.Event.OnMuteClick)
                    }
                    .background(Color.White)
                    .size(42.dp),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )

            if (state.chatInfo.groupType == ChatResponseItemGroupType.DUAL) {
                Image(
                    painterResource(R.drawable.ic_camera),
                    "",
                    modifier = Modifier
                        .clickable {
                            onEventSent(CommunicationRoomContract.Event.OnVideoClick)
                        }
                        .background(Color.White)
                        .size(42.dp),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
                )
            }

            val speakerIcon =
                if (state.isUsingSpeaker) R.drawable.ic_speaker else R.drawable.ic_head_speaker
            Image(
                painterResource(speakerIcon),
                "",
                modifier = Modifier
                    .clickable {
                        onEventSent(CommunicationRoomContract.Event.OnSpeakerClick)
                    }
                    .background(Color.White)
                    .size(42.dp),
                colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
            )

            if (state.isShowingSelfCameraPreview) {
                Image(
                    painterResource(R.drawable.ic_camera_switch),
                    "",
                    modifier = Modifier
                        .clickable {
                            onEventSent(CommunicationRoomContract.Event.OnCameraSwitchClick)
                        }
                        .background(Color.White)
                        .size(42.dp),
                    colorFilter = ColorFilter.tint(colorResource(id = R.color.purple_500))
                )
            }

            val communicatingMembers = state.chatInfo.members.filter { it.isCommunicating }
            if (communicatingMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Members:",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.background(Color.White),
                )

                communicatingMembers.forEach {
                    Text(
                        text = "* ${it.username}",
                        color = Color.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.background(Color.White),
                    )
                }
            }
        }
    }
}