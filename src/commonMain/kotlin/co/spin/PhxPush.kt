package co.spin

import kotlinx.serialization.json.JSON

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
        //private val channel: PhxChannel,
        private val event: String,
        /*!< Holds the payload that will be sent to the server. */
        private val payload: JSON
) {

    /*!< Name of event for the message. */
    private val refEvent: String? = null

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
    private var receivedResp: JSON? = null

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
    private fun cancelRefEvent() = Unit

    /**
     *  \brief Cancels After callback from possibly triggering.
     *
     *  \return void
     */
    private fun cancelAfter() = Unit

    /**
     *  \brief Starts the timer until After Callback is triggered.
     *
     *  \return void
     */
    private fun startAfter() = Unit

    /**
     *  \brief Central function that kicks off OnMessage callbacks.
     *
     *  \param payload Payload to match against.
     *  \return void
     */
    private fun matchReceive(payload: JSON) = Unit

    /**
     *  \brief Sets the payload that this class will push out through
     * Websockets.
     *
     *  \param payload
     *  \return void
     */
    fun setPayload(payload: JSON) = Unit


    /**
     *  \brief Sends Phoenix Formatted message with payload through Websockets.
     *
     *  \return void
     */
    fun send() = Unit

    /**
     *  \brief Adds a callback to be triggered for status.
     *
     *  Adds a callback to be triggered when message matching status is posted.
     *
     *  \param status The status that callback should respond to.
     *  \param callback The callback triggered when status message is posted.
     *  \return std::shared_ptr<PhxPush>
     */
    fun onReceive(status: String, callback: OnMessage) : PhxPush =  TODO()

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
    fun after(ms: Int, callback: After) : PhxPush = TODO()
};
