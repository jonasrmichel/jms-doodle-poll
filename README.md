jms-doodle-poll
===============

This is a [Doodle](http://doodle.com/)-like calendar poll system using the Java Message Service’s point-to-point model (as implemented by [Joram](http://joram.ow2.org/)).

<h2>Usage</h2>
The poll system must be started from the command line.

Configure the path settings of the shared available users file in <code>samples/src/joram/doodle/Settings.java</code>.
Point <code>AVAILABLE_USERS_PATH</code> to the directory containing the <code>users</code> file.

From the command line, <code>cd</code> into the joram directory.
<pre><code>$ cd /path/to/this/directory/samples/src/joram</code></pre>

Build the project.
<pre><code>$ ant clean compile</code></pre>

Launch the Joram server.
<pre><code>$ ant reset single_server</code></pre>

Run the doodle system's administrative configuration once.
<pre><code>$ ant doodle_admin</code></pre>

Launch as many users as you want. Use the following command to launch a single user.
<pre><code>$ ant doodle_user</code></pre>

<h2>Requirements</h2>
Requires Java 1.7+. The necessary parts of the Joram JMS implementation have been included.
