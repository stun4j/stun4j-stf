package com.stun4j.stf.boot;

import static com.stun4j.stf.core.utils.Asserts.state;

/**
 * Stf DataSource configuration
 * <p>
 * 
 * @author Jay Meng
 */
public class Datasource {
  private String beanName;

  /**
   * Whether or not the dataSource is allowed to be automatically created. In general, the created
   * dataSource is also registered with the container
   * <p>
   * Default: false
   */
  private boolean autoCreateEnabled = false;

  /**
   * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
   */
  private String driverClassName;

  /**
   * JDBC URL of the database.
   */
  private String url;

  /**
   * Login username of the database.
   */
  private String username;

  /**
   * Login password of the database.
   */
  private String password;

  private final Object parentBind;

  public boolean hasAnyBasicJdbcPropertyConfigured() {
    return url != null || username != null || password != null || driverClassName != null;
  }

  public String getBeanName() {
    return beanName;
  }

  public void setBeanName(String beanName) {
    this.beanName = (beanName == null ? beanName : beanName.trim());
  }

  public boolean isAutoCreateEnabled() {
    return autoCreateEnabled;
  }

  public void setAutoCreateEnabled(boolean autoCreateEnabled) {
    state(!(parentBind instanceof Core),
        "Auto create is not allowed for 'Stf Core DataSource' > The core datasource should be provided by user-side, Stf's manner is to use it, not create it.");

    this.autoCreateEnabled = autoCreateEnabled;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  Datasource(Object parentBind) {
    this.parentBind = parentBind;
  }
}
