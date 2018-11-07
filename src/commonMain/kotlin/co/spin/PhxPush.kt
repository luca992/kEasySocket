package co.spin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content

/**
 *  \brief Constructor
 *
 *  \param channel The Phoenix Channel to send to.
 *  \param event The Phoenix Event to post to.
 *  \param payload The Payload to send.
 *  \return PhxPush
 */
class PhxPush(
        /*!< The event name the server listens on. */
        private val channel: PhxChannel,
        private val event: String,
        /*!< Holds the payload that will be sent to the server. */
        private var payload: JsonElement
) {
    /*!< Name of event for the message. */
    private var refEvent: String? = null

    /*!< The callback to trigger if event is not returned from server. */
    private var afterHook: After? = null

    /*!< The interval to wait before triggering afterHook. */
    private var afterInterval: Int = 0

    /*!<
     * recHooks contains a list of tuples where Item 1 is the Status
     * and Item 2 is the callback.
     */
    private val recHooks =  mutableListOf<Pair<String, OnMessage>>()

    /*!< The response from server if server responded to sent message. */
    private var receivedResp: JsonElement? = null

    /*!< Flag determining whether or not the message was sent through Sockets.
     */
    private var sent = false

    /*!< Mutex used when setting this->shouldContinueAfterCallback. */
    //std::mutex afterTimerMutex;

    /*!< Flag that determines if After callback will be triggered. */
    private var shouldContinueAfterCallback = false

    /**
     *  \brief Stops listening for this event.
     *
     *  \return void
     */
    private fun cancelRefEvent() {
        channel.offEvent(refEvent)
    }

    /**
     *  \brief Cancels After callback from possibly triggering.
     *
     *  \return void
     */
    private fun cancelAfter() {
        if (afterHook == null) {
            return
        }

        GlobalScope.launch(TDispatchers.Default) {
            //std::lock_guard<std::mutex> guard(this->afterTimerMutex);
            shouldContinueAfterCallback = false
        }
    }

    /**
     *  \brief Starts the timer until After Callback is triggered.
     *
     *  \return void
     */
    private fun startAfter() {
        if (afterHook == null) {
            return
        }

        // FIXME: Should this be weak?
        val interval = afterInterval
        GlobalScope.launch(TDispatchers.Default) {
            // Use sleep_for to wait specified time (or sleep_until).
            shouldContinueAfterCallback = true
            delay(interval*1000L /*interval in seconds*/)
            //std::lock_guard<std::mutex> guard(this->afterTimerMutex);
            if (shouldContinueAfterCallback) {
                cancelRefEvent()
                afterHook?.invoke()
            shouldContinueAfterCallback = false
            }
        }
    }

    /**
     *  \brief Central function that kicks off OnMessage callbacks.
     *
     *  \param payload Payload to match against.
     *  \return void
     */
    private fun matchReceive(payload: JsonObject) {
        for (recHook in  recHooks) {
            if (recHook.first == payload.get("status").content) {
                recHook.second.invoke(payload.getObject("response"))
            }
        }
    }

    /**
     *  \brief Sets the payload that this class will push out through
     * Websockets.
     *
     *  \param payload
     *  \return void
     */
    fun setPayload(payload: JsonObject) {
        this.payload = payload
    }


    /**
     *  \brief Sends Phoenix Formatted message with payload through Websockets.
     *
     *  \return void
     */
    fun send() {
        val ref = channel.socket
        refEvent = channel.replyEventName(ref.toString().toLong())//FIXME: ("MAYBE don't do this...)
        receivedResp = null
        sent = false

        // FIXME: Should this be weak?
        channel.onEvent(refEvent!!) {
            message: JsonElement,
            _: Long ->
            receivedResp = message
            matchReceive(message as JsonObject)
            cancelRefEvent()
            cancelAfter()
        }


        startAfter()
        sent = true

        // clang-format off
        /*channel.socket.push(receivedResp
        { { "topic", this->channel->getTopic() },
            { "event", this->event },
            { "payload", this->payload },
            { "ref", ref }
        })*/
    }

    /**
     *  \brief Adds a callback to be triggered for status.
     *
     *  Adds a callback to be triggered when message matching status is posted.
     *
     *  \param status The status that callback should respond to.
     *  \param callback The callback triggered when status message is posted.
     *  \return std::shared_ptr<PhxPush>
     */
    fun onReceive(status: String, callback: OnMessage) : PhxPush {
            // receivedResp could actually be a std::string.
        if (receivedResp is JsonObject
            && (receivedResp as JsonObject).getValue("status").content == status) {
            callback(receivedResp as JsonObject)
        }
        recHooks.add(recHooks.size, Pair(status, callback))
        return this
    }

    /**
     *  \brief Adds a callback to be triggered if event doesn't come back.
     *
     *  Adds a callback to be triggered after ms if event is not `replied back`
     * to.
     *  If PhxPush receives a message with matching event, callback will not
     *  be called.
     *
     *  \param ms Milliseconds to wait before triggering callback.
     *  \param callback Callback to be triggered after ms has passed.
     *  \return std::shared_ptr<PhxPush>
     */
    fun after(ms: Int, callback: After) : PhxPush {
        if (afterHook!=null) {
            // ERROR
        }

        afterInterval = ms
        afterHook = callback
        return this
    }
};
