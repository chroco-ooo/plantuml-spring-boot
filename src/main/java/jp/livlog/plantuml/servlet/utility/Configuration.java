/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * Project Info:  https://plantuml.com
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package jp.livlog.plantuml.servlet.utility;

import java.io.IOException;
import java.util.Properties;

/**
 * Shared PlantUML Server configuration.
 */
public final class Configuration {

    /**
     * Singleton configuration instance.
     */
    private static Configuration instance;

    /**
     * Configuration properties.
     */
    private final Properties     config;

    /**
     * Singleton constructor.
     */
    private Configuration() {

        this.config = new Properties();

        // Default values
        this.config.setProperty("SHOW_SOCIAL_BUTTONS", "off");
        this.config.setProperty("SHOW_GITHUB_RIBBON", "off");
        // End of default values

        try {
            final var is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            if (is != null) {
                this.config.load(is);
                is.close();
            }
        } catch (final IOException e) {
            // Just log a warning
            e.printStackTrace();
        }
    }


    /**
     * Get the configuration.
     *
     * @return the complete configuration
     */
    public static Properties get() {

        if (Configuration.instance == null) {
            Configuration.instance = new Configuration();
        }
        return Configuration.instance.config;
    }


    /**
     * Get a boolean configuration value.
     *
     * @param key config property key
     *
     * @return true if the value is "on"
     */
    public static boolean get(final String key) {

        if (Configuration.get().getProperty(key) == null) {
            return false;
        }
        return Configuration.get().getProperty(key).startsWith("on");
    }

}
