import net.researchgate.release.ReleaseExtension

plugins {
    id("net.researchgate.release") version "3.0.2"
}

configure<ReleaseExtension> {
  tagTemplate.set("v\${version}")
}

