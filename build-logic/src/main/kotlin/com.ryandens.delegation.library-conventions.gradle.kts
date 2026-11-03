import com.ryandens.delegation.AutoDelegatePublishingPlugin

plugins {
    id("com.ryandens.delegation.java-conventions")
    `java-library`
}

// The binary plugin lives in this build, so apply its class after script compilation.
pluginManager.apply(AutoDelegatePublishingPlugin::class.java)
