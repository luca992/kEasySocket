package co.spin

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 *  \brief Constructor
 *
 *  \param socket The socket connection PhxChannel sends messages over.
 *  \param topic The topic this Channel sends and receives messages for.
 *  \param params Params to send up to channel.
 *  \return PhxChannel
 */
class PhxChannel(
        /*!< The socket connection to send and receive data over. */
        val socket: PhxSocket,
        /*!< The topic of this channel. */
        val topic: String,
        /*! Params that will be sent up as a payload to Phoenix Channel. */
        val params: Map<String, String>) {

    /*!<
     * bindings contains a list of tuples where Item 1 is the Event
     * and Item 2 is the callback.
     */
    private var bindings = mutableListOf<Pair<String, OnReceive>>()

    /*!< A flag indicating whether there has been an attempt to join channel. */
    private var joinedOnce = false

    /*!< The PhxPush object that is responsible for joining a channel. */
    private var joinPush: PhxPush? = null

    /*!< Unused, supposed to be used for PhxChannelDelegate callbacks. */
    //PhxChannelDelegate* delegate;


    /*!< The current state of the channel. */
    private var state = ChannelState.CLOSED

    /**
     *  \brief Trigger joining of channel.
     *
     *  \return void
     */
    private fun sendJoin() {
        state = ChannelState.JOINING
        joinPush!!.setPayload(JsonObject(params.mapValues { JsonPrimitive(it.value) }))
        joinPush!!.send()
    }

    /**
     *  \brief Wrap sendJoin.
     *
     *  \return void
     */
    private fun rejoin() {
        if (joinedOnce && state != ChannelState.JOINING
                && state != ChannelState.JOINED) {
            sendJoin()
        }
    }

    /**
     *  \brief Determines if Channel is part of topic.
     *
     *  \param topic The topic to check against.
     *  \return bool Indicating if member of topic.
     */
    private fun isMemberOfTopic(topic: String) : Boolean {
        return this.topic == topic
    }

    /**
     *  \brief Trigger callbacks that match event.
     *
     *  \param event The event to trigger callbacks for.
     *  \param message The message to forward to callback.
     *  \param ref The ref of the message.
     *  \return void
     */
    fun triggerEvent(event: String, message: JsonElement, ref: Long) {
        // Trigger OnReceive callbacks that match event.
        for (b in bindings) {
            if (b.first == event) {
                b.second.invoke(message, ref)
            }
        }
    }


    /**
     *  \brief Creates a event named for a reply using ref.
     *
     *  \param ref Phoenix ref.
     *  \return std::string
     */
    fun replyEventName(ref: Long) : String  {
        return "chan_reply_$ref"
    }


    /**
     *  \brief Called to bootstrap PhxChannel and PhxSocket together.
     *
     *  This MUST be called for PhxChannel communication to work.
     *
     *  The reason for the existance of this function is because of the need
     *  to set up shared_ptrs between PhxChannel and PhxSocket.
     *  This initialization code can't be in the constructor because we're
     *  trying to use this->shared_from_this() which can only be used after a
     *  shared_ptr is already created which won't work if we're calling
     *  this->shared_from_this from the constructor.
     *
     *  \return void
     */
    fun bootstrap() {
        // NOTE: This can't be done in the constructor.
        // So we bootstrap the connection here.
        socket.addChannel(this)

        socket.onOpen { rejoin() }

        socket.onClose {
            state = ChannelState.CLOSED;
            socket.removeChannel(this)
        }

        socket.onError { state = ChannelState.ERRORED }

        joinPush = PhxPush(this, "phx_join", JsonObject(params.mapValues { JsonPrimitive(it.value) }))

        joinPush!!.onReceive("ok") { state = ChannelState.JOINED }

        onEvent("phx_reply") { message: JsonElement, ref: Long ->
            triggerEvent(replyEventName(ref), message, ref)
        }
    }

    /**
     *  \brief Sends a join message to Phoenix Channel.
     *
     *  \return std::shared_ptr<PhxPush>
     */
    fun join() : PhxPush{
        if (joinedOnce) {
            // ERROR
        } else {
            joinedOnce = true;
        }

        sendJoin()
        return joinPush!!
    }

    /**
     *  \brief Closes the Phoenix Channel connection.
     *
     *  \return void
     */
    fun leave() {
        state = ChannelState.CLOSED
        var payload = JsonObject(mapOf())
        val p = pushEvent("phx_leave", payload)
        p.onReceive("ok") {
            triggerEvent("phx_close", JsonPrimitive("leave"), -1);
        }
    }

    /**
     *  \brief Adds event and callback to this->bindings.
     *
     *  Adding an event to bindings causes its callback to get triggered
     *  when its corresponding event is posted.
     *
     *  \param event The event to listen to.
     *  \param callback The callback to trigger if event is posted.
     *  \return void
     */
    fun onEvent(event: String, callback: OnReceive) {
        bindings.add(Pair(event, callback))
    }

    /**
     *  \brief Removes event from this->bindings.
     *
     *  Removing event from bindings skips any callback associated with that
     *  event from triggering.
     *
     *  \param event The event to unsubscribe.
     *  \return void
     */
    fun offEvent(event: String?){
        // Remove all Event bindings that match event.
        bindings =  bindings.filter { it.first != event }.toMutableList()
    }

    /**
     *  \brief Adds a callback that will get triggered on close.
     *
     *  \param callback The callback triggered on close.
     *  \return void
     */
    fun onClose(callback: OnClose) {
        onEvent("phx_close") {
            message: JsonElement,
            ref: Long ->
            callback(message.toString())
        }
    }

    /**
     *  \brief Adds a callback that will get triggered on error.
     *
     *  \param callback The callback triggered on error.
     *  \return void
     */
    fun onError(callback: OnError) {
        onEvent("phx_error") {
            error: JsonElement,
            ref: Long ->
            callback(error.toString())
        }
    }

    /**
     *  \brief Pushes an event over Websockets.
     *
     *  \param event The event to push to server.
     *  \param payload Payload to push to server.
     *  \return std::shared_ptr<PhxPush>
     */
    fun pushEvent(event : String, payload: JsonObject) : PhxPush {
        val p = PhxPush(this, event, payload)
        p.send()
        return p
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PhxChannel

        if (topic != other.topic) return false

        return true
    }

    override fun hashCode(): Int {
        return topic.hashCode()
    }


}